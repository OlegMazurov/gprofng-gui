
Summary: A GNU gprofng GUI collection.
Name: binutils-gprofng-gui
Version: 2.39.50
Release: 24.0.1%{?dist}
License: GPLv3+
URL: https://sourceware.org/binutils
Source: https://ftp.gnu.org/gnu/binutils/gprofng-gui-%{version}.tar.xz
BuildRequires: autoconf automake make sed coreutils

# We need java-1.8.0 or late to build and run.
# How can I set it in the .spec file ?
#BuildRequires: java
#Requires: java

Requires: binutils = %{version}-%{release}

Summary: GUI part for gprofng
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
%post
exit 0

#----------------------------------------------------------------------------
%preun
exit 0

#----------------------------------------------------------------------------
%files
%{_bindir}/gp-display-gui
%{_datadir}/gprofng-tools/gprofng-analyzer.jar
%{_datadir}/gprofng-tools/gprofng-collector.jar
%{_datadir}/gprofng-tools/gprofng.jar

#----------------------------------------------------------------------------
%changelog
* Thu Oct 27 2022 Vladimir Mezentsev <vladimir.mezentsev@oracle.com>
- First version being package

