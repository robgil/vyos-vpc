Configuration for vyos to connect to Amazon VPC VPN

# Use cases
## Inter Region Routing / Connectivity
Having secure connectivity between regions is a big deal for many enterprises. VPCs are great for private connectivity, but if you want to talk between them in different regions, you need to turn up something. This is a way to do that.

Essentially, in AWS there is no VRRP or gratuitous ARP. The challenge in AWS is handling failover for your gateway. Granted you could do an interface up/down type of situation with ENI devices, but I find this a bit cumbersome and not supported by most appliance vendors. One way to get this redundancy is to create a routing only VPC and turn up IPSec/BGP tunnels/neighbors using the VPC VPN (CGW+VGW). This will give you the redundancy you need to an individual VPC. Then do the same in the other region. Once both regions are routing between the routing VPC and the other VPC, you can then do an IPSec tunnel and BGP peer between the two setups in the different regions.
```
---------       ------------------                      -------------------       ---------
| VPC 1 | <===> | Router VPC East| <===> INTERNET <===> | Router VPC West | <===> | VPC 2 |
---------       ------------------                      -------------------       ---------
```

- Why a router VPC?
- AWS only advertises the VPC ip range from the VPC VPN. You wouldn't want to readvertise that back to itself. 

## Inter VPC Routing / Connectivity
This is more tricky because AWS only offers 2 peers per region for VPC VPN connectivity. This means you can't have multiple VPCs using the same Router (at least I have not found a way to do this). If there was a way to have multiple tunnels with the same peer, then you could set up multiple EIPs on the same router to use as the CGWs. Unfortunately I don't know of a way to do this on a single router. It is possible to do on separate routers. So essentially you would have two routers per VPC. If you assign separate ASs to each pair of VPC routers, you can do shortest path goodness, especially if you have VPCs in other regions.  

There is a workaround however, but will limit how many VPCs you can configure this way. Basically AWS suggests creating VGWs until you get new ips.

[AWS Docs on VPC Routing](https://aws.amazon.com/articles/5458758371599914)

The other way they suggest is with VRF. If you have the cash, I think the only virtual software router that can do this is the cisco one. 

# Config
The config.boot file contains an example config for connecting and routing to an Amazon VPC through an IPSec routed tunnel with BGP.

# Output of running tunnels
```
vyos@VyOS-AMI:~$ show vpn ipsec sa
Peer ID / IP                            Local ID / IP               
------------                            -------------
207.171.167.234                         10.0.1.30                              

    Description: mgmt-dev1

    Tunnel  State  Bytes Out/In   Encrypt  Hash    NAT-T  A-Time  L-Time  Proto
    ------  -----  -------------  -------  ----    -----  ------  ------  -----
    vti     up     21.7K/21.8K    aes128   sha1    no     2747    3600    all

 
Peer ID / IP                            Local ID / IP               
------------                            -------------
207.171.167.235                         10.0.1.30                              

    Description: mgmt-dev2

    Tunnel  State  Bytes Out/In   Encrypt  Hash    NAT-T  A-Time  L-Time  Proto
    ------  -----  -------------  -------  ----    -----  ------  ------  -----
    vti     up     4.7K/4.7K      aes128   sha1    no     1177    3600    all

 
vyos@VyOS-AMI:~$ show vpn ike sa  
Peer ID / IP                            Local ID / IP               
------------                            -------------
207.171.167.234                         10.0.1.30                              

    Description: mgmt-dev1

    State  Encrypt  Hash    D-H Grp  NAT-T  A-Time  L-Time
    -----  -------  ----    -------  -----  ------  ------
    up     aes128   sha1    2        no     17834   28800  

 
Peer ID / IP                            Local ID / IP               
------------                            -------------
207.171.167.235                         10.0.1.30                              

    Description: mgmt-dev2

    State  Encrypt  Hash    D-H Grp  NAT-T  A-Time  L-Time
    -----  -------  ----    -------  -----  ------  ------
    up     aes128   sha1    2        no     17145   28800  

 
```

# Gotchas
- The configs from AWS don't specify your local-address. You need to add this.
- The configs from AWS also don't include the id and remote-id. These are needed to identify the PSK thats used for the ipsec tunnel.
- NAT traveral needs to be enabled in AWS. Every EIP is a NAT. 
- AWS uses the same peer addresses for all VPCs in a region. You cannot create multiple tunnels to the same peer ip. Which means you cant route to multiple VPCs on the same router. :(
- Adding virtual interfaces does not allow you to create multiple tunnels to the same peer.

# Why VyOS
I generally prefer CentOS/RHEL for most stuff and for most stuff its great. Unfortunately, networking is extremely far behind. Especially for things like virtual tunnel interfaces. It is very complicated trying to get this to work with netkey. Netkey (replacement for Klips which had the ipsec0 interfaces) is essentially a second routing table above the network routing table on the box. It is overly complicated IMO and I still don't understand why they went that route (no puns). It causes insane things like the possibility of sending your default gateway traffic over the VPN tunnel. Essentially disconnecting you from the network. 

VyOS supports virtual tunnel interfaces and is purpose built for networking. Also the junos style cli is much nicer than the quagga/zebra cli. Its nice to be able to commit, compare, rollback etc.  
