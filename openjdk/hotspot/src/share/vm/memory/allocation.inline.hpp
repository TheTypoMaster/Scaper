/*
 * Copyright 1997-2005 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

// Explicit C-heap memory management

void trace_heap_malloc(size_t size, const char* name, void *p);
void trace_heap_free(void *p);


// allocate using malloc; will fail if no memory available
inline char* AllocateHeap(size_t size, const char* name = NULL) {
  char* p = (char*) os::malloc(size);
  #ifdef ASSERT
  if (PrintMallocFree) trace_heap_malloc(size, name, p);
  #else
  Unused_Variable(name);
  #endif
  if (p == NULL) vm_exit_out_of_memory(size, name);
  return p;
}

inline char* ReallocateHeap(char *old, size_t size, const char* name = NULL) {
  char* p = (char*) os::realloc(old,size);
  #ifdef ASSERT
  if (PrintMallocFree) trace_heap_malloc(size, name, p);
  #else
  Unused_Variable(name);
  #endif
  if (p == NULL) vm_exit_out_of_memory(size, name);
  return p;
}

inline void FreeHeap(void* p) {
  #ifdef ASSERT
  if (PrintMallocFree) trace_heap_free(p);
  #endif
  os::free(p);
}
