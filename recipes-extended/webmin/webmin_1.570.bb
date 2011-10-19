DESCRIPTION = "Web-based administration interface"
HOMEPAGE="http://www.webmin.com"
LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://LICENCE;md5=0373ac9f611e542ddebe1ec6394afc3c"

# FIXME: some of this should be figured out automatically
RDEPENDS_${PN} += "perl perl-module-socket perl-module-exporter perl-module-exporter-heavy perl-module-carp perl-module-strict"
RDEPENDS_${PN} += "perl-module-warnings perl-module-warnings-register perl-module-xsloader perl-module-posix perl-module-autoloader"
RDEPENDS_${PN} += "perl-module-fcntl perl-module-tie-hash perl-module-vars perl-module-time-local perl-module-config perl-module-constant"
RDEPENDS_${PN} += "perl-module-file perl-module-file-glob perl-module-sdbm perl-module-sdbm-file"

PR = "r0"

SRC_URI = "${SOURCEFORGE_MIRROR}/webadmin/webmin-${PV}.tar.gz \
          file://setup.sh"

inherit allarch perlnative update-rc.d

do_configure() {
    # Remove binaries and plugins for other platforms
    rm -rf acl/Authen-SolarisRBAC-0.1*
    rm -rf {format,{bsd,hpux,sgi}exports,zones,rbac,smf,ipfw,ipfilter,dfsadmin}
    rm -f mount/{free,net,open}bsd-mounts*
    rm -f mount/macos-mounts*

    # Remove some plugins for the moment
    rm -rf lilo frox wuftpd telnet pserver cpan shorewall webalizer cfengine fsdump pap
    rm -rf majordomo fetchmail sendmail mailboxes procmail filter mailcap dovecot exim spam qmailadmin postfix
    rm -rf stunnel squid sarg pptp-client pptp-server jabber openslp sentry cluster-* vgetty burner heartbeat

    # Adjust configs
    mv init/config-debian-linux init/config-generic-linux
    sed -i "s/shutdown_command=.*/shutdown_command=poweroff/" init/config-generic-linux

    # Fix insane naming that causes problems at packaging time (must be done before deleting below)
    find . -name "*\**" -exec rename \* ALL {} \;

    # Remove some other files we don't need
    find . -name "config-*" -a \! -name "config-generic-linux" -a \! -name "config-ALL-linux" -a \! -name "*.pl" -delete
    rm -f webmin-gentoo-init

    # Don't need these at runtime (and we have our own setup script)
    rm -f setup.sh
    rm -f setup.pl

    # Use pidof for finding PIDs
    sed -i "s/find_pid_command=.*/find_pid_command=pidof NAME/" config-generic-linux
}

INITSCRIPT_NAME = "webmin"
INITSCRIPT_PARAMS = "start 99 5 3 2 . stop 10 0 1 6 ."

do_install() {
    install -d ${D}${sysconfdir}
    install -d ${D}${sysconfdir}/webmin
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 webmin-init ${D}${sysconfdir}/init.d/webmin

    install -d ${D}${localstatedir}
    install -d ${D}${localstatedir}/webmin

    install -d ${D}${libexecdir}/webmin
    cp -pPR ${S}/* ${D}${libexecdir}/webmin
    rm -f ${D}${libexecdir}/webmin/webmin-init

    # Run setup script
    export perl=perl
    export perl_runtime=${bindir}/perl
    export prefix=${D}
    export tempdir=${S}/install_tmp
    export wadir=${libexecdir}/webmin
    export config_dir=${sysconfdir}/webmin
    export var_dir=${localstatedir}/webmin
    export os_type=generic-linux
    export os_version=0
    export real_os_type="${DISTRO_NAME}"
    export real_os_version="${DISTRO_VERSION}"
    export port=10000
    export login=admin
    export password=password
    export ssl=0
    export atboot=1
    export no_pam=1
    mkdir -p $tempdir
    ${S}/../setup.sh
}

PACKAGES_DYNAMIC += "webmin-module-*"
RRECOMMENDS_${PN} += "webmin-module-system-status"

RDEPENDS_webmin-module-proc = "procps"
RDEPENDS_webmin-module-raid = "mdadm"
RDEPENDS_webmin-module-exports = "perl-module-file-basename perl-module-file-path perl-module-cwd perl-module-file-spec perl-module-file-spec-unix"
RRECOMMENDS_webmin-module-fdisk = "parted"
RRECOMMENDS_webmin-module-lvm = "lvm2"

python populate_packages_prepend() {
	import os, os.path

	wadir = bb.data.expand('${libexecdir}/webmin', d)
	wadir_image = bb.data.expand('${D}', d) + wadir
	modules = []
	for mod in os.listdir(wadir_image):
		modinfo = os.path.join(wadir_image, mod, "module.info")
		if os.path.exists(modinfo):
			modules.append(mod)
	
	do_split_packages(d, wadir, '^(%s)$' % "|".join(modules), 'webmin-module-%s', 'Webmin module for %s', allow_dirs=True, prepend=True)
}

# Time-savers
EXCLUDE_FROM_SHLIBS = "1"
split_and_strip_files() {
    :
}

package_do_pkgconfig() {
    :
}