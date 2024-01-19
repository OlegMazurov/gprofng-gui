## Process this file with automake to generate Makefile.in
#
#   Copyright (C) 2012-2024 Free Software Foundation, Inc.
#
# This file is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; see the file COPYING3.  If not see
# <http://www.gnu.org/licenses/>.
#

docdir = $(srcdir)/doc

# Options to extract the man page
MANCONF = -Dman

TEXI2POD = perl $(srcdir)/etc/texi2pod.pl $(AM_MAKEINFOFLAGS)
POD2MAN = pod2man --center="User Commands" \
	--release="gprofng-gui-$(VERSION)" --section=1

#info_TEXINFOS       = gp-display-gui.texi
TEXINFO_TEX         = .
MAKEINFOHTML        = $(MAKEINFO) --html --no-split

man_MANS = gp-display-gui.1

# Build the man page from the texinfo file
# The sed command removes the no-adjust Nroff command so that
# the man output looks standard.
$(man_MANS): $(docdir)/gp-macros.texi
	$(AM_V_GEN)touch $@
	( nm=`basename $@ .1` ; \
	  $(AM_V_at)$(TEXI2POD) $(MANCONF) < $(docdir)/$$nm.texi > $$nm.pod ; \
	  $(AM_V_at)($(POD2MAN) $$nm.pod | sed -e '/^.if n .na/d' > $@.tmp && \
	    mv -f $@.tmp $@) || (rm -f $@.tmp && exit 1) ; \
	  $(AM_V_at)rm -f $$nm.pod )

gp-display-gui.1: $(docdir)/gp-display-gui.texi

EXTRA_DIST += $(man_MANS) $(docdir)/gp-macros.texi $(docdir)/gp-display-gui.texi
CLEANFILES += $(man_MANS)

