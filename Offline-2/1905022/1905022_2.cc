/*
 *
 * Network topology:
 *
 * S0-------------R1-------------R2--------------Rec
 *      10Mbps          1 Mbps          10Mbps
 *        2ms             5ms             2ms
 * Calculate throughput for this network at 5ms interval and plot a throughput vs time graph
 */
#include "ns3/applications-module.h"
#include "ns3/core-module.h"
#include "ns3/internet-module.h"
#include "ns3/network-module.h"
#include "ns3/point-to-point-module.h"
#include "ns3/stats-module.h"
#include "ns3/csma-module.h"
#include "ns3/mobility-module.h"
#include "ns3/traffic-control-module.h"
#include "ns3/ssid.h"
#include "ns3/wifi-module.h"
#include "ns3/yans-wifi-helper.h"
#include "ns3/flow-monitor-helper.h"
#include "ns3/netanim-module.h"
#include <fstream>

NS_LOG_COMPONENT_DEFINE("offline_1");

using namespace ns3;
using namespace std;


double sentPackets;
double receivedPackets;
int droppedPackets;
double totalBytes;
Ptr<PacketSink> sink;     //!< Pointer to the packet sink application
Ptr<OutputStreamWrapper> stream;


void TxTrigger (Ptr<const Packet> p)
{
  sentPackets++;
}
void RxTrigger (Ptr<const Packet> p,const Address &address)
{
  receivedPackets++;
  totalBytes += p->GetSize ()*8.0;
}

void
RxDrop(Ptr<const Packet> p)
{
   droppedPackets++;
}

int
main(int argc, char* argv[])
{
    int payloadSize ;//= 1472;           /* Transport layer payload size in bytes. */
    std::string dataRate = "1Mbps";      /* Application layer datarate. */
    int dataRateInt = 1;//000000;                 /* Application layer datarate. */
    double simulationTime = 10;            /* Simulation time in seconds. */

    int  totalNodes;//=20;//40,60,80,100
    int numFlow;//=20;//20 30 40 50
    int packetPerSecond;// = 100;
    double speed;//=atoi(argv[1]);
    sentPackets = 0.0;
    receivedPackets = 0.0;
    droppedPackets = 0;
    totalBytes = 0.0;
    int packetSize=1024;

    int parametrized=0; //set the variable parameter

    string fileName1,fileName2;

    if(argc>1)
    {
        totalNodes=atoi(argv[1]);
        numFlow=atoi(argv[2]);
        packetPerSecond=atoi(argv[3]);
        speed=atoi(argv[4]);
        parametrized=atoi(argv[5]);
        fileName1=argv[6];
        fileName2=argv[7];
        
    }
    else
    {
        totalNodes=20;
        numFlow=20;
        speed=10;
        packetPerSecond=100;
        parametrized=1;
    }
    

    payloadSize= (packetSize*128*dataRateInt)/packetPerSecond;

    AsciiTraceHelper asciiTraceHelper;
    stream  = asciiTraceHelper.CreateFileStream("offline_1.dat");
  
    ofstream throughput2 (fileName1, std::ios::out | std::ios::app);
    ofstream packetRatio2 (fileName2, std::ios::out | std::ios::app);

    // Create gateways, sources, and sinks
    NodeContainer gateway;
    gateway.Create(2);
    NodeContainer senders;
    senders.Create (totalNodes);
    NodeContainer receivers;
    receivers.Create (totalNodes);
    NodeContainer SenderApNodes=gateway.Get(0);
    NodeContainer ReceiverApNodes=gateway.Get(1);


    string gatewayDataRate = "1Mbps";
    string gatewayDelay = "5ms";

    PointToPointHelper bottleneck;
    bottleneck.SetDeviceAttribute ("DataRate", StringValue (gatewayDataRate));
    bottleneck.SetChannelAttribute ("Delay", StringValue (gatewayDelay));



    YansWifiChannelHelper channelSenders = YansWifiChannelHelper::Default();
    YansWifiChannelHelper channelReceivers = YansWifiChannelHelper::Default();
    YansWifiPhyHelper phySenders,phyReceivers;
    phySenders.SetChannel(channelSenders.Create());
    phyReceivers.SetChannel(channelReceivers.Create());

   

  
    WifiHelper wifi;
    WifiMacHelper macSenders,macReceivers;
    Ssid ssidSenders = Ssid("ns-3-ssid");
    Ssid ssidReceivers = Ssid("ns-3-ssid");


    // Hold the PointToPointNetDevices created
    NetDeviceContainer bottleneckDevices;
    bottleneckDevices = bottleneck.Install(gateway);


    NetDeviceContainer wifiSenderStaDevices,wifiSenderApDevices;
    macSenders.SetType("ns3::StaWifiMac", "Ssid", SsidValue(ssidSenders), "ActiveProbing", BooleanValue(false));
    wifiSenderStaDevices = wifi.Install(phySenders, macSenders, senders);
    macSenders.SetType("ns3::ApWifiMac","Ssid",SsidValue(ssidSenders));
    wifiSenderApDevices=wifi.Install(phySenders,macSenders,SenderApNodes);

    NetDeviceContainer wifiReceiverStaDevices,wifiReceiverApDevices;
    macReceivers.SetType("ns3::StaWifiMac", "Ssid", SsidValue(ssidReceivers), "ActiveProbing", BooleanValue(false));
    wifiReceiverStaDevices = wifi.Install(phyReceivers, macReceivers, receivers);
    macReceivers.SetType("ns3::ApWifiMac","Ssid",SsidValue(ssidReceivers));
    wifiReceiverApDevices=wifi.Install(phyReceivers,macReceivers,ReceiverApNodes);

   

    //mobile 


    MobilityHelper mobility;

    mobility.SetPositionAllocator ("ns3::GridPositionAllocator",
                                 "MinX", DoubleValue (0.0),
                                 "MinY", DoubleValue (0.0),
                                 "DeltaX", DoubleValue (0.1),
                                 "DeltaY", DoubleValue (0.1), //error with changing deltax ,deltay
                                 "GridWidth", UintegerValue (3),
                                 "LayoutType", StringValue ("RowFirst"));

  // // tell STA nodes how to move
  
   
   mobility.SetMobilityModel ("ns3::RandomWalk2dMobilityModel",
                             "Bounds", RectangleValue (Rectangle (-20,20,-20,20)),
                             "Speed", StringValue ("ns3::ConstantRandomVariable[Constant="+std::to_string(speed)+"]"));
    mobility.Install(senders);
    mobility.Install(receivers);
  
  
    mobility.SetMobilityModel("ns3::ConstantPositionMobilityModel");
    // mobility.SetVelocity(Vector(10.0, 0, 0));
     
    mobility.Install(SenderApNodes);
    mobility.Install(ReceiverApNodes);


    InternetStackHelper stack;
    stack.Install(senders);
    stack.Install(receivers);
    stack.Install(SenderApNodes);
    stack.Install(ReceiverApNodes);


    Ipv4AddressHelper address;
    address.SetBase("10.1.1.0", "255.255.255.0"); 
    Ipv4InterfaceContainer senderStaInterfaces = address.Assign(wifiSenderStaDevices);
    Ipv4InterfaceContainer senderApInterfaces = address.Assign(wifiSenderApDevices);

    address.SetBase("10.1.2.0", "255.255.255.0"); 
    Ipv4InterfaceContainer bottleneckInterfaces = address.Assign(bottleneckDevices); 

    address.SetBase("10.1.3.0", "255.255.255.0"); 
    Ipv4InterfaceContainer receiverStaInterfaces = address.Assign(wifiReceiverStaDevices);
    Ipv4InterfaceContainer receiverApInterfaces = address.Assign(wifiReceiverApDevices);



    /* Populate routing table */
    


    PacketSinkHelper sinkHelper("ns3::TcpSocketFactory",InetSocketAddress(Ipv4Address::GetAny(), 9));
    ApplicationContainer sinkApp ;//= sinkHelper.Install(receivers.Get(i));


    for(int i=0;i<totalNodes;i++)
    {

        sinkApp.Add(sinkHelper.Install(receivers.Get(i)));
        sinkApp.Start(Seconds(0.0));
    }
   
    OnOffHelper senderHelper("ns3::TcpSocketFactory", (InetSocketAddress(receiverStaInterfaces.GetAddress(1), 9)));
    senderHelper.SetAttribute("PacketSize", UintegerValue(payloadSize));
    senderHelper.SetAttribute("OnTime", StringValue("ns3::ConstantRandomVariable[Constant=1]"));
    senderHelper.SetAttribute("OffTime", StringValue("ns3::ConstantRandomVariable[Constant=0]"));
    senderHelper.SetAttribute("DataRate", DataRateValue(DataRate(dataRate)));
   
    ApplicationContainer senderApp;
    int currFlows = 0;
    for (int i = 0; i < totalNodes; i++){
        // Create an on/off app on right side node which sends packets to the left side
        AddressValue remoteAddress (InetSocketAddress (receiverStaInterfaces.GetAddress(i), 9));
        for(int j = 0; j < totalNodes; j++){
            senderHelper.SetAttribute ("Remote", remoteAddress);
            senderApp.Add(senderHelper.Install(senders.Get(j)));
            currFlows++;
            if(currFlows >= numFlow) break;
        }
    }
    senderApp.Start(Seconds(1.0));
            

    for(int i=0; i<totalNodes; i++)
    {
        senderApp.Get(i)->TraceConnectWithoutContext("Tx", MakeCallback(&TxTrigger));
    }

    for(int i=0; i<totalNodes; i++)
    {
        sinkApp.Get(i)->TraceConnectWithoutContext("Rx", MakeCallback(&RxTrigger));
    }

     for(int i=0; i<totalNodes; i++)
    {
        sinkApp.Get(i)->TraceConnectWithoutContext("Drop", MakeCallback(&RxDrop));
    }


    Ipv4GlobalRoutingHelper::PopulateRoutingTables();
  

    /* Start Simulation */
    Simulator::Stop(Seconds(simulationTime + 1));
    Simulator::Run();

    double throughput = totalBytes/(1e6 * simulationTime);
    // cout<<"chk "<<receivedPackets<<" "<<sentPackets<<endl;
    double ratio=1.0;
    ratio*=receivedPackets/sentPackets;
    Simulator::Destroy();
   


    if(parametrized==1)
    {
        throughput2<<totalNodes<<" "<<throughput<<endl;
        packetRatio2<<totalNodes<<" "<<ratio<<endl;
    }
    else if(parametrized==2)
    {
        throughput2<<numFlow<<" "<<throughput<<endl;
        packetRatio2<<numFlow<<" "<<ratio<<endl;
    }

    else if(parametrized==3)
    {
        throughput2<<packetPerSecond<<" "<<throughput<<endl;
        packetRatio2<<packetPerSecond<<" "<<ratio<<endl;
    }

    else if(parametrized==4)
    {
        throughput2<<speed<<" "<<throughput<<endl;
        packetRatio2<<speed<<" "<<ratio<<endl;
    }

    return 0;


}



