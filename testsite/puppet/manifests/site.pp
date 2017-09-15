include stdlib
include apt
include wget
include ::openssl
include archive
include augeas
include php

# Apache server and modules installation
class { 'apache': 
  mpm_module => 'prefork',
}

class { 'apache::mod::rewrite': }

class { 'apache::mod::ssl': }

class { 'apache::mod::php': }

# Make site dir
exec { 'mkdir site':
  path => [ "/bin/", "/sbin/" , "/usr/bin/", "/usr/sbin/" ],
  command => "mkdir -p /var/www/site",
}

apache::vhost { 'xpath_finer':
  port    => '80',
  servername => 'localhost',
  docroot => '/var/www/site',
  docroot_owner => 'www-data',
  docroot_group => 'www-data',
  override => ['All'],
  require => Exec['mkdir site'],
}

exec { "disable default" :
  path => [ "/bin/", "/sbin/" , "/usr/bin/", "/usr/sbin/" ],
  command => "a2dissite 15-default.conf",
  require => Class['apache'],
}

exec { "copy site" :
  path => [ "/bin/", "/sbin/" , "/usr/bin/", "/usr/sbin/" ],
  command => "cp -R /home/vagrant/site/* /var/www/site",
  require => Exec['disable default'],
}

