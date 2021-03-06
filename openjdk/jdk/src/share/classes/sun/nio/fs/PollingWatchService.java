/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.nio.fs;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import com.sun.nio.file.SensitivityWatchEventModifier;

/**
 * Simple WatchService implementation that uses periodic tasks to poll
 * registered directories for changes.  This implementation is for use on
 * operating systems that do not have native file change notification support.
 */

class PollingWatchService
    extends AbstractWatchService
{
    // map of registrations
    private final Map<Object,PollingWatchKey> map =
        new HashMap<Object,PollingWatchKey>();

    // used to execute the periodic tasks that poll for changes
    private final ScheduledExecutorService scheduledExecutor;

    PollingWatchService() {
        // TBD: Make the number of threads configurable
        scheduledExecutor = Executors
            .newSingleThreadScheduledExecutor(new ThreadFactory() {
                 @Override
                 public Thread newThread(Runnable r) {
                     Thread t = new Thread(r);
                     t.setDaemon(true);
                     return t;
                 }});
    }

    /**
     * Register the given file with this watch service
     */
    @Override
    WatchKey register(final Path path,
                      WatchEvent.Kind<?>[] events,
                      WatchEvent.Modifier... modifiers)
         throws IOException
    {
        // check events - CCE will be thrown if there are invalid elements
        if (events.length == 0)
            throw new IllegalArgumentException("No events to register");
        final Set<WatchEvent.Kind<?>> eventSet =
            new HashSet<WatchEvent.Kind<?>>(events.length);
        for (WatchEvent.Kind<?> event: events) {
            // standard events
            if (event == StandardWatchEventKind.ENTRY_CREATE ||
                event == StandardWatchEventKind.ENTRY_MODIFY ||
                event == StandardWatchEventKind.ENTRY_DELETE)
            {
                eventSet.add(event);
                continue;
            }

            // OVERFLOW is ignored
            if (event == StandardWatchEventKind.OVERFLOW) {
                if (events.length == 1)
                    throw new IllegalArgumentException("No events to register");
                continue;
            }

            // null/unsupported
            if (event == null)
                throw new NullPointerException("An element in event set is 'null'");
            throw new UnsupportedOperationException(event.name());
        }

        // A modifier may be used to specify the sensitivity level
        SensitivityWatchEventModifier sensivity = SensitivityWatchEventModifier.MEDIUM;
        if (modifiers.length > 0) {
            for (WatchEvent.Modifier modifier: modifiers) {
                if (modifier == null)
                    throw new NullPointerException();
                if (modifier instanceof SensitivityWatchEventModifier) {
                    sensivity = (SensitivityWatchEventModifier)modifier;
                    continue;
                }
                throw new UnsupportedOperationException("Modifier not supported");
            }
        }

        // check if watch service is closed
        if (!isOpen())
            throw new ClosedWatchServiceException();

        // registration is done in privileged block as it requires the
        // attributes of the entries in the directory.
        try {
            final SensitivityWatchEventModifier s = sensivity;
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<PollingWatchKey>() {
                    @Override
                    public PollingWatchKey run() throws IOException {
                        return doPrivilegedRegister(path, eventSet, s);
                    }
                });
        } catch (PrivilegedActionException pae) {
            Throwable cause = pae.getCause();
            if (cause != null && cause instanceof IOException)
                throw (IOException)cause;
            throw new AssertionError(pae);
        }
    }

    // registers directory returning a new key if not already registered or
    // existing key if already registered
    private PollingWatchKey doPrivilegedRegister(Path path,
                                                 Set<? extends WatchEvent.Kind<?>> events,
                                                 SensitivityWatchEventModifier sensivity)
        throws IOException
    {
        // check file is a directory and get its file key if possible
        BasicFileAttributes attrs = Attributes.readBasicFileAttributes(path);
        if (!attrs.isDirectory()) {
            throw new NotDirectoryException(path.toString());
        }
        Object fileKey = attrs.fileKey();
        if (fileKey == null)
            throw new AssertionError("File keys must be supported");

        // grab close lock to ensure that watch service cannot be closed
        synchronized (closeLock()) {
            if (!isOpen())
                throw new ClosedWatchServiceException();

            PollingWatchKey watchKey;
            synchronized (map) {
                watchKey = map.get(fileKey);
                if (watchKey == null) {
                    // new registration
                    watchKey = new PollingWatchKey(this, path, fileKey);
                    map.put(fileKey, watchKey);
                } else {
                    // update to existing registration
                    watchKey.disable();
                }
            }
            watchKey.enable(events, sensivity.sensitivityValueInSeconds());
            return watchKey;
        }

    }

    @Override
    void implClose() throws IOException {
        synchronized (map) {
            for (Map.Entry<Object,PollingWatchKey> entry: map.entrySet()) {
                PollingWatchKey watchKey = entry.getValue();
                watchKey.disable();
                watchKey.invalidate();
            }
            map.clear();
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                scheduledExecutor.shutdown();
                return null;
            }
         });
    }

    /**
     * Entry in directory cache to record file last-modified-time and tick-count
     */
    private static class CacheEntry {
        private long lastModified;
        private int lastTickCount;

        CacheEntry(long lastModified, int lastTickCount) {
            this.lastModified = lastModified;
            this.lastTickCount = lastTickCount;
        }

        int lastTickCount() {
            return lastTickCount;
        }

        long lastModified() {
            return lastModified;
        }

        void update(long lastModified, int tickCount) {
            this.lastModified = lastModified;
            this.lastTickCount = tickCount;
        }
    }

    /**
     * WatchKey implementation that encapsulates a map of the entries of the
     * entries in the directory. Polling the key causes it to re-scan the
     * directory and queue keys when entries are added, modified, or deleted.
     */
    private class PollingWatchKey extends AbstractWatchKey {
        private final Path dir;
        private final Object fileKey;

        // current event set
        private Set<? extends WatchEvent.Kind<?>> events;

        // the result of the periodic task that causes this key to be polled
        private ScheduledFuture<?> poller;

        // indicates if the key is valid
        private volatile boolean valid;

        // used to detect files that have been deleted
        private int tickCount;

        // map of entries in directory
        private Map<Path,CacheEntry> entries;

        PollingWatchKey(PollingWatchService watcher,
                        Path dir,
                        Object fileKey)
            throws IOException
        {
            super(watcher);
            this.dir = dir;
            this.fileKey = fileKey;
            this.valid = true;
            this.tickCount = 0;
            this.entries = new HashMap<Path,CacheEntry>();

            // get the initial entries in the directory
            DirectoryStream<Path> stream = dir.newDirectoryStream();
            try {
                for (Path entry: stream) {
                    // don't follow links
                    long lastModified = Attributes
                        .readBasicFileAttributes(entry, LinkOption.NOFOLLOW_LINKS)
                        .lastModifiedTime().toMillis();
                    entries.put(entry.getName(),
                                new CacheEntry(lastModified, tickCount));
                }
            } catch (ConcurrentModificationException cme) {
                // thrown if directory iteration fails
                Throwable cause = cme.getCause();
                if (cause != null && cause instanceof IOException)
                    throw (IOException)cause;
                throw new AssertionError(cme);
            } finally {
                stream.close();
            }
        }

        FileRef directory() {
            return dir;
        }

        Object fileKey() {
            return fileKey;
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        void invalidate() {
            valid = false;
        }

        // enables periodic polling
        void enable(Set<? extends WatchEvent.Kind<?>> events, long period) {
            synchronized (this) {
                // update the events
                this.events = events;

                // create the periodic task
                Runnable thunk = new Runnable() { public void run() { poll(); }};
                this.poller = scheduledExecutor
                    .scheduleAtFixedRate(thunk, period, period, TimeUnit.SECONDS);
            }
        }

        // disables periodic polling
        void disable() {
            synchronized (this) {
                if (poller != null)
                    poller.cancel(false);
            }
        }

        @Override
        public void cancel() {
            valid = false;
            synchronized (map) {
                map.remove(fileKey());
            }
            disable();
        }

        /**
         * Polls the directory to detect for new files, modified files, or
         * deleted files.
         */
        synchronized void poll() {
            if (!valid) {
                return;
            }

            // update tick
            tickCount++;

            // open directory
            DirectoryStream<Path> stream = null;
            try {
                stream = dir.newDirectoryStream();
            } catch (IOException x) {
                // directory is no longer accessible so cancel key
                cancel();
                signal();
                return;
            }

            // iterate over all entries in directory
            try {
                for (Path entry: stream) {
                    long lastModified = 0L;
                    try {
                        lastModified = Attributes
                            .readBasicFileAttributes(entry, LinkOption.NOFOLLOW_LINKS)
                            .lastModifiedTime().toMillis();
                    } catch (IOException x) {
                        // unable to get attributes of entry. If file has just
                        // been deleted then we'll report it as deleted on the
                        // next poll
                        continue;
                    }

                    // lookup cache
                    CacheEntry e = entries.get(entry.getName());
                    if (e == null) {
                        // new file found
                        entries.put(entry.getName(),
                                     new CacheEntry(lastModified, tickCount));

                        // queue ENTRY_CREATE if event enabled
                        if (events.contains(StandardWatchEventKind.ENTRY_CREATE)) {
                            signalEvent(StandardWatchEventKind.ENTRY_CREATE, entry.getName());
                            continue;
                        } else {
                            // if ENTRY_CREATE is not enabled and ENTRY_MODIFY is
                            // enabled then queue event to avoid missing out on
                            // modifications to the file immediately after it is
                            // created.
                            if (events.contains(StandardWatchEventKind.ENTRY_MODIFY)) {
                                signalEvent(StandardWatchEventKind.ENTRY_MODIFY, entry.getName());
                            }
                        }
                        continue;
                    }

                    // check if file has changed
                    if (e.lastModified != lastModified) {
                        if (events.contains(StandardWatchEventKind.ENTRY_MODIFY)) {
                            signalEvent(StandardWatchEventKind.ENTRY_MODIFY, entry.getName());
                        }
                    }
                    // entry in cache so update poll time
                    e.update(lastModified, tickCount);

                }
            } catch (ConcurrentModificationException x) {
                // FIXME - should handle this
            } finally {

                // close directory stream
                try {
                    stream.close();
                } catch (IOException x) {
                    // ignore
                }
            }

            // iterate over cache to detect entries that have been deleted
            Iterator<Map.Entry<Path,CacheEntry>> i = entries.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Path,CacheEntry> mapEntry = i.next();
                CacheEntry entry = mapEntry.getValue();
                if (entry.lastTickCount() != tickCount) {
                    Path name = mapEntry.getKey();
                    // remove from map and queue delete event (if enabled)
                    i.remove();
                    if (events.contains(StandardWatchEventKind.ENTRY_DELETE)) {
                        signalEvent(StandardWatchEventKind.ENTRY_DELETE, name);
                    }
                }
            }
        }
    }
}
