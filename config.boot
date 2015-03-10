interfaces {
    ethernet eth0 {
        address dhcp
        duplex auto
        hw-id 0a:0c:9b:49:69:0c
        smp_affinity auto
        speed auto
    }
    loopback lo {
    }
    vti vti0 {
        address 169.254.255.38/30
        description "VPC tunnel 1"
        mtu 1436
    }
    vti vti1 {
        address 169.254.255.34/30
        description "VPC tunnel 2"
        mtu 1436
    }
}
protocols {
    bgp 65000 {
        neighbor 169.254.255.33 {
            remote-as 7224
            soft-reconfiguration {
                inbound
            }
            timers {
                holdtime 30
                keepalive 30
            }
        }
        neighbor 169.254.255.37 {
            remote-as 7224
            soft-reconfiguration {
                inbound
            }
            timers {
                holdtime 30
                keepalive 30
            }
        }
        network 10.0.0.0/16 {
        }
        network 10.0.1.0/24 {
        }
    }
}
service {
    ssh {
        disable-password-authentication
        port 22
    }
}
system {
    config-management {
        commit-revisions 20
    }
    console {
        device ttyS0 {
            speed 9600
        }
    }
    host-name VyOS-AMI
    login {
        user vyos {
            authentication {
                encrypted-password "*"
                public-keys vpc {
                    key xxxxxx
                    type ssh-rsa
                }
            }
            level admin
        }
    }
    ntp {
        server 0.pool.ntp.org {
        }
        server 1.pool.ntp.org {
        }
        server 2.pool.ntp.org {
        }
    }
    package {
        auto-sync 1
        repository community {
            components main
            distribution helium
            password ""
            url http://packages.vyos.net/vyos
            username ""
        }
    }
    syslog {
        global {
            facility all {
                level notice
            }
            facility protocols {
                level debug
            }
        }
    }
    time-zone UTC
}
vpn {
    ipsec {
        esp-group AWS {
            compression disable
            lifetime 3600
            mode tunnel
            pfs enable
            proposal 1 {
                encryption aes128
                hash sha1
            }
        }
        ike-group AWS {
            dead-peer-detection {
                action restart
                interval 15
                timeout 30
            }
            key-exchange ikev1
            lifetime 28800
            proposal 1 {
                dh-group 2
                encryption aes128
                hash sha1
            }
        }
        ipsec-interfaces {
            interface eth0
        }
        nat-traversal enable
        site-to-site {
            peer 207.171.167.234 {
                authentication {
                    id <EIP of this router>
                    mode pre-shared-secret
                    pre-shared-secret xxxxx
                    remote-id 207.171.167.234 # AWS US-East VPN Endpoint 1
                }
                connection-type initiate
                description "VPC tunnel 1"
                ike-group AWS
                local-address 10.0.1.30 # Private ip of this router
                vti {
                    bind vti0
                    esp-group AWS
                }
            }
            peer 207.171.167.235 {
                authentication {
                    id <EIP of this router>
                    mode pre-shared-secret
                    pre-shared-secret xxxxx
                    remote-id 207.171.167.235 # AWS US-East VPN Endpoint 2
                }
                connection-type initiate
                description "VPC tunnel 2"
                ike-group AWS
                local-address 10.0.1.30 # Private ip of this router
                vti {
                    bind vti1
                    esp-group AWS
                }
            }
        }
    }
}


/* Warning: Do not remove the following line. */
/* === vyatta-config-version: "cluster@1:config-management@1:conntrack-sync@1:conntrack@1:cron@1:dhcp-relay@1:dhcp-server@4:firewall@5:ipsec@4:nat@4:qos@1:quagga@2:system@6:vrrp@1:wanloadbalance@3:webgui@1:webproxy@1:zone-policy@1" === */
/* Release version: VyOS 1.1.0 */
