#
# Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Sun designates this
# particular file as subject to the "Classpath" exception as provided
# by Sun in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
# CA 95054 USA or visit www.sun.com if you need additional information or
# have any questions.
#

#
# Modularizing the JDK
#

BUILDDIR = ..
include $(BUILDDIR)/common/Defs.gmk

CLASSANALYZER_JAR=$(BUILDTOOLJARDIR)/classanalyzer.jar
JAVA_FLAGS=$(JAVA_TOOLS_FLAGS) -Xbootclasspath:$(CLASSBINDIR)

MODULE_LIB = $(ABS_OUTPUTDIR)/modules
MAINMANIFEST=$(JDK_TOPDIR)/make/tools/manifest.mf
MODULE_JAR_MANIFEST_FILE=$(ABS_TEMPDIR)/manifest.tmp

TMP=$(ABS_TEMPDIR)/modules
MODULE_CLASSLIST = $(TMP)/classlist
MODULE_CLASSES = $(TMP)/classes
MODULE_LIST = $(MODULE_CLASSLIST)/modules.list

# Modules in the modules/ext directory
EXT_MODULES = localedata security-sunec security-sunjce

# Build PKCS#11 on all platforms except 64-bit Windows.
# We exclude windows-amd64 because we don't have any
# 64-bit PKCS#11 implementations to test with on that platform.
PKCS11 = security-sunpkcs11
ifeq ($(ARCH_DATA_MODEL), 64)
  ifeq ($(PLATFORM), windows)
    PKCS11 =
  endif
endif

EXT_MODULES += $(PKCS11)

# Build Microsoft CryptoAPI provider only on (non-64-bit) Windows platform.
ifeq ($(PLATFORM), windows)
  ifneq ($(ARCH_DATA_MODEL), 64)
    EXT_MODULES += security-sunmscapi
  endif
endif


JDK_MODULES = tools

SUBDIRS = tools
all build clean clobber::
	$(SUBDIRS-loop)

all:: unpack-jars gen-classlist modularize

$(CLASSANALYZER_JAR):
	$(CD) tools && $(MAKE) all

JAR_LIST := $(shell $(FIND) $(ABS_OUTPUTDIR)/lib -name \*.jar -depth) 
unpack-jars:
	$(RM) -rf $(MODULE_CLASSES)
	$(MKDIR) -p $(MODULE_CLASSES)
	$(CP) -rf $(CLASSBINDIR)/* $(MODULE_CLASSES)
	for jf in  $(JAR_LIST) ; do \
	   $(CD) $(MODULE_CLASSES) && $(BOOT_JAR_CMD) xf $$jf $(BOOT_JAR_JFLAGS);\
	done

gen-classlist: $(CLASSANALYZER_JAR)
	@$(ECHO) ">>>Making "$@" @ `$(DATE)` ..."
	@$(RM) -rf $(MODULE_CLASSLIST)
	@$(MKDIR) -p $(MODULE_CLASSLIST)

	@# Use java in the default tool directory.
	@# OUTPUTDIR for solaris 64-bit doesn't have the tools.
	$(JAVA_TOOLS_DIR)/java $(JAVA_FLAGS) \
		-Dclassanalyzer.debug \
		-jar $(CLASSANALYZER_JAR) \
		-jdkhome $(OUTPUTDIR) \
	        -config modules.config \
	        -config modules.group \
		-depconfig jdk7.depconfig \
		-depconfig optional.depconfig \
		-showdynamic \
		-output $(MODULE_CLASSLIST)
	@$(ECHO) ">>>Finished making "$@" @ `$(DATE)` ..."

modularize: $(MODULE_JAR_MANIFEST_FILE)
	@$(ECHO) ">>>Making "$@" @ `$(DATE)` ..."
	@$(RM) -rf $(MODULE_LIB)
	@$(MKDIR) -p $(MODULE_LIB)/lib
	@$(MKDIR) -p $(MODULE_LIB)/jre/lib/ext

	@# create modules
	for m in `$(NAWK) '{print $$1}' $(MODULE_LIST)` ; do \
   	   $(ECHO) "Creating module $$m" ; \
   	   $(SED) -e 's%\\%\/%g' < $(MODULE_CLASSLIST)/$$m.classlist > $(TMP)/tmp.cf ; \
	   if [ -f $(MODULE_CLASSLIST)/$$m.resources ] ; then \
   	       $(SED) -e 's%\\%\/%g' < $(MODULE_CLASSLIST)/$$m.resources >> $(TMP)/tmp.cf ; \
           fi ; \
	   $(CD) $(MODULE_CLASSES) && \
               $(BOOT_JAR_CMD) c0mf $(MODULE_JAR_MANIFEST_FILE) \
		   $(MODULE_LIB)/$$m.jar \
		   @$(TMP)/tmp.cf \
                   $(BOOT_JAR_JFLAGS) ; \
	done
	@$(CD) $(MODULE_CLASSES) && $(java-vm-cleanup)
	@# move modules to lib, jre/lib, or jre/lib/ext
	for m in $(EXT_MODULES) ; do \
	   $(MV) $(MODULE_LIB)/$$m.jar $(MODULE_LIB)/jre/lib/ext ; \
	done
	for m in $(JDK_MODULES) ; do \
	   $(MV) $(MODULE_LIB)/$$m.jar $(MODULE_LIB)/lib ; \
	done
	$(MV) $(MODULE_LIB)/*.jar $(MODULE_LIB)/jre/lib
	@$(ECHO) ">>>Finished making "$@" @ `$(DATE)` ..."

$(MODULE_JAR_MANIFEST_FILE):
	$(SED) -e "s/@@RELEASE@@/$(RELEASE)/" $(MAINMANIFEST) > $@

clean clobber::
	$(RM) -rf $(MODULE_CLASSLIST)
	$(RM) -rf $(MODULE_LIB)
	$(RM) -f $(MODULE_JAR_MANIFEST_FILE)
	$(RM) -f $(CLASSANALYZER_JAR)

