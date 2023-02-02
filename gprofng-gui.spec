
Summary: Graphical User Interface for the gprofng profiler.
Name: gprofng-gui
Version: 2.40
Release: 1%{?dist}
License: GPLv3+
URL: FIX_ME:https://www.gnu.org/software/gprofng-gui
Source: FIX_ME:https://ftp.gnu.org/gnu/gprofng-gui/gprofng-gui-%{version}.tar.xz
BuildRequires: autoconf automake make sed coreutils
BuildRequires: java-devel

Requires: binutils-gprofng >= %{Version}
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

