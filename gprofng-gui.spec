# Copyright (C) 2022, 2023 Free Software Foundation
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.

Summary: Graphical User Interface for the gprofng profiler.
Name: gprofng-gui
Version: 1.0
Release: 1%{?dist}
License: GPLv3+
URL: https://www.gnu.org/software/gprofng-gui
Source: https://ftp.gnu.org/gnu/gprofng-gui/gprofng-gui-%{version}.tar.xz
BuildRequires: autoconf automake make sed coreutils
BuildRequires: java-devel

Requires: binutils-gprofng >= 2.40
Requires: java-devel

Provides: gprofng-gui = %{version}-%{release}

%description
The GNU gprofng GUI is a feature rich graphical user interface for the GNU
gprofng tool. It makes it possible to interactively analyze and compare gprofng
profiling experiments. Users can drill into an applications profile together
with the applications code to gather an understanding and insight into
what an application is doing throughout it's runtime.

%global debug_package %{nil} 

#----------------------------------------------------------------------------
%prep
%setup 

#----------------------------------------------------------------------------
%build
%configure
%make_build

#----------------------------------------------------------------------------
%install
%make_install DESTDIR=%{buildroot}


#----------------------------------------------------------------------------
%files
%{_bindir}/gp-display-gui
%{_datadir}/%{name}/gprofng-analyzer.jar
%{_datadir}/%{name}/gprofng-collector.jar
%{_datadir}/%{name}/gprofng.jar

#----------------------------------------------------------------------------
%changelog
* Thu Oct 27 2022 Vladimir Mezentsev <vladimir.mezentsev@oracle.com>
- First version being package

