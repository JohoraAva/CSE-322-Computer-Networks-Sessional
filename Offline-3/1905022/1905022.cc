/*
 *
 *
 */
#include "ns3/applications-module.h"
#include "ns3/core-module.h"
#include "ns3/internet-module.h"
#include "ns3/network-module.h"
#include "ns3/point-to-point-module.h"
#include "ns3/stats-module.h"
#include "ns3/mobility-module.h"
#include "ns3/flow-monitor-module.h"
#include "ns3/point-to-point-layout-module.h"
#include <fstream>



using namespace ns3;
using namespace std;

NS_LOG_COMPONENT_DEFINE ("offline3");


class TcpNewApp : public Application
{
public:
  TcpNewApp ();
  virtual ~TcpNewApp ();

  /**
   * Register this type.
   * \return The TypeId.
   */
  static TypeId GetTypeId (void);
  void Setup (Ptr<Socket> socket, Address address, uint32_t packetSize, DataRate dataRate, uint32_t simultime);

private:
  virtual void StartApplication (void);
  virtual void StopApplication (void);

  void ScheduleTx (void);
  void SendPacket (void);

  Ptr<Socket>     m_socket;
  Address         m_peer;
  uint32_t        m_packetSize;
  DataRate        m_dataRate;
  EventId         m_sendEvent;
  bool            m_running;
  uint32_t        m_packetsSent;
  uint32_t        m_simultime;
};

TcpNewApp::TcpNewApp ()
  : m_socket (0),
    m_peer (),
    m_packetSize (0),
    m_dataRate (0),
    m_sendEvent (),
    m_running (false),
    m_packetsSent (0),
    m_simultime (0)
{
}

TcpNewApp::~TcpNewApp ()
{
  m_socket = 0;
}

/* static */
TypeId TcpNewApp::GetTypeId (void)
{
  static TypeId tid = TypeId ("TcpNewApp")
    .SetParent<Application> ()
    .SetGroupName ("Tutorial")
    .AddConstructor<TcpNewApp> ()
    ;
  return tid;
}

void
TcpNewApp::Setup (Ptr<Socket> socket, Address address, uint32_t packetSize, DataRate dataRate, uint32_t simultime)
{
  m_socket = socket;
  m_peer = address;
  m_packetSize = packetSize;
  m_dataRate = dataRate;
  m_simultime = simultime;
}


void
TcpNewApp::StartApplication (void)
{
  m_running = true;
  m_packetsSent = 0;
    if (InetSocketAddress::IsMatchingType (m_peer))
    {
      m_socket->Bind ();
    }
  else
    {
      m_socket->Bind6 ();
    }
  m_socket->Connect (m_peer);
  SendPacket ();
}

void
TcpNewApp::StopApplication (void)
{
  m_running = false;

  if (m_sendEvent.IsRunning ())
    {
      Simulator::Cancel (m_sendEvent);
    }

  if (m_socket)
    {
      m_socket->Close ();
    }
}

void
TcpNewApp::SendPacket (void)
{
  Ptr<Packet> packet = Create<Packet> (m_packetSize);
  m_socket->Send (packet);

  if(Simulator::Now().GetSeconds() < m_simultime) ScheduleTx();
}

void
TcpNewApp::ScheduleTx (void)
{
  if (m_running)
    {
      Time tNext (Seconds (m_packetSize * 8 / static_cast<double> (m_dataRate.GetBitRate ())));
      m_sendEvent = Simulator::Schedule (tNext, &TcpNewApp::SendPacket, this);
    }
}

static void
CwndChange (Ptr<OutputStreamWrapper> stream, uint32_t oldCwnd, uint32_t newCwnd)
{
  *stream->GetStream () << Simulator::Now ().GetSeconds () << " " << newCwnd << std::endl;
}


int main(int argc, char* argv[])
{
    uint32_t payloadSize = 1400;           /* Transport layer payload size in bytes. */ //---
    std::string otherDataRate = "1Gbps";      /* Application layer otherDataRate. */
    std::string bottleneckDelay="100ms";    /* Bottleneck delay. */
    std::string otherDelay="1ms";            /* Application layer delay. */
    // int otherDataRateInt=1000;                   /* Application layer otherDataRate in Mbps. */
    int bottleneckDelayInt=100;             /* Bottleneck delay in ms. */
    // int otherDelayInt=1;                    /* Application layer delay in ms. */
    double simulationTime = 10;            /* Simulation time in seconds. */
    double packetSize=1000;
    //bool verbose = true;
    int nSender = 2;
    int nReceiver = 2;
    int nFlows=2;
    std::string tcpVariant1 = "ns3::TcpNewReno"; // tcp variant 1
    std::string tcpVariant2 = "ns3::TcpAdaptiveReno"; //tcp variant 2
    std::string output_folder="output"; //output folder


    //input
    std::string bottleneckDataRate="50Mbps";
    int bottleneckDataRateInt=50;
    int power=6;
    double packetLossRate;// = 0.000001;
    std::string fileName1;


    if(argc>1)
    {
        bottleneckDataRateInt=std::stoi(argv[1]);
        bottleneckDataRate=argv[1];//+"Mbps";
        bottleneckDataRate.append("Mbps");
        power=std::stoi(argv[2]);
        packetLossRate=std::pow(10,-power);
        fileName1=argv[3];
    }
    else
    {
        bottleneckDataRateInt=50;
        bottleneckDataRate="50Mbps";
        packetLossRate=0.000001;
        fileName1="output.dat";
    }


    std::ofstream outputFile (fileName1, std::ios::out | std::ios::app);
    
    Config::SetDefault ("ns3::TcpSocket::SegmentSize", UintegerValue (payloadSize));

    PointToPointHelper bottleneck;
    bottleneck.SetDeviceAttribute ("DataRate", StringValue (bottleneckDataRate));
    bottleneck.SetChannelAttribute ("Delay", StringValue (bottleneckDelay));



    PointToPointHelper others;
    others.SetDeviceAttribute ("DataRate", StringValue (otherDataRate));
    others.SetChannelAttribute ("Delay", StringValue (otherDelay));
    others.SetQueue ("ns3::DropTailQueue", "MaxSize", StringValue (std::to_string ((bottleneckDelayInt * bottleneckDataRateInt)/packetSize) + "p"));

    PointToPointDumbbellHelper dumbbell (nSender, others, nReceiver, others, bottleneck);

    // cout<<"bottleneckDataRate : "<<bottleneckDataRate<<" ; bottleneckDelay : "<<bottleneckDelay<<" ; otherDataRate : "<<otherDataRate<<" ; otherDelay : "<<otherDelay<<endl;
    //add error model

    Ptr<RateErrorModel> errorModel= CreateObject<RateErrorModel> ();
    errorModel->SetAttribute ("ErrorRate", DoubleValue (packetLossRate));
    dumbbell.m_routerDevices.Get(1)->SetAttribute ("ReceiveErrorModel", PointerValue (errorModel));



    // stackinstall
    // tcp variant 1
    Config::SetDefault ("ns3::TcpL4Protocol::SocketType", StringValue (tcpVariant1));
    InternetStackHelper stack1;
    for (uint32_t i = 0; i < dumbbell.LeftCount (); i+=2)
    {
        stack1.Install (dumbbell.GetLeft (i)); // left nodes
    }
    for (uint32_t i = 0; i < dumbbell.RightCount (); i+=2)
    {
        stack1.Install (dumbbell.GetRight (i)); // right nodes
    }
    //installing stack in routers
    stack1.Install (dumbbell.GetLeft ());
    stack1.Install (dumbbell.GetRight ());


    // tcp variant 2

    Config::SetDefault ("ns3::TcpL4Protocol::SocketType", StringValue (tcpVariant2));
    InternetStackHelper stack2;
    for (uint32_t i = 1; i < dumbbell.LeftCount (); i+=2)
    {
        stack2.Install (dumbbell.GetLeft (i)); // left nodes
    }
    for (uint32_t i = 1; i < dumbbell.RightCount (); i+=2)
    {
        stack2.Install (dumbbell.GetRight (i)); // right nodes
    }




    //set ip address
    dumbbell.AssignIpv4Addresses (Ipv4AddressHelper ("10.1.1.0", "255.255.255.0"), // left nodes
                          Ipv4AddressHelper ("10.2.1.0", "255.255.255.0"),  // right nodes
                          Ipv4AddressHelper ("10.3.1.0", "255.255.255.0")); // bottleneck nodes
    Ipv4GlobalRoutingHelper::PopulateRoutingTables (); // populate routing table


    //install flow monitor
    FlowMonitorHelper flowMonitor;
    Ptr<FlowMonitor> monitor = flowMonitor.InstallAll ();


    for(int i=0;i<nFlows;i++)
    {
        Address sinkAddress (InetSocketAddress (dumbbell.GetRightIpv4Address (i), 9));
        PacketSinkHelper sinkHelper("ns3::TcpSocketFactory",InetSocketAddress(dumbbell.GetRightIpv4Address (i), 9));
        ApplicationContainer sinkApps =sinkHelper.Install (dumbbell.GetRight (i));
        sinkApps.Start (Seconds (0));
        sinkApps.Stop (Seconds (simulationTime));

        Ptr<Socket> ns3TcpSocket = Socket::CreateSocket (dumbbell.GetLeft (i), TcpSocketFactory::GetTypeId ());
        Ptr<TcpNewApp> app = CreateObject<TcpNewApp> ();
        app->Setup (ns3TcpSocket, sinkAddress, payloadSize, DataRate (otherDataRate), simulationTime);
        dumbbell.GetLeft (i)->AddApplication (app);
        app->SetStartTime (Seconds (1));
        app->SetStopTime (Seconds (simulationTime));

        std::ostringstream oss;
        oss << "scratch/output/output"<< i+1 <<  ".dat";
        AsciiTraceHelper asciiTraceHelper;
        Ptr<OutputStreamWrapper> stream = asciiTraceHelper.CreateFileStream (oss.str());
        ns3TcpSocket->TraceConnectWithoutContext ("CongestionWindow", MakeBoundCallback (&CwndChange, stream));
    }
    Simulator::Stop (Seconds (simulationTime));
    Simulator::Run ();


     // flow monitor statistics
    int j = 0;
    float avgThroughput = 0;
    float curThroughput = 0;
    float curPacketLossRate = 0;
    float curThroughputArr[] = {0, 0}; //for 2 algo

    double jainIndex0 = 0;
    double jainIndex1 = 0;


    // variables
    uint32_t SentPackets = 0;
    uint32_t ReceivedPackets = 0;
    uint32_t LostPackets = 0;

    Ptr<Ipv4FlowClassifier> classifier = DynamicCast<Ipv4FlowClassifier> (flowMonitor.GetClassifier ());
    monitor->CheckForLostPackets ();
    FlowMonitor::FlowStatsContainer stats = monitor->GetFlowStats ();
    for (auto i=stats.begin ();i!=stats.end();i++)
    {
      curThroughput=i->second.rxBytes*8.0/((simulationTime)*1000);
      curPacketLossRate=(i->second.lostPackets)*100.0/i->second.txPackets;
      if(j%2)
      {
        curThroughputArr[1] += i->second.rxBytes;
      }
      else
      {
        curThroughputArr[0] += i->second.rxBytes;
      }

      SentPackets = SentPackets +(i->second.txPackets);
      ReceivedPackets = ReceivedPackets + (i->second.rxPackets);
      LostPackets = LostPackets + (i->second.lostPackets);
      avgThroughput+=curThroughput;
      j++;

      //fairness
      jainIndex0 += curThroughput;
      jainIndex1 += (curThroughput * curThroughput);
    }

    double jainIndex = (jainIndex0 * jainIndex0) / ( j * jainIndex1);
    curThroughputArr[0] /= ((simulationTime )*1000);
    curThroughputArr[1] /= ((simulationTime )*1000);
    avgThroughput = curThroughputArr[0] + curThroughputArr[1];


    double lostPacketRatio= (LostPackets*100.00)/SentPackets;

    outputFile<<bottleneckDataRateInt<<" "<<packetLossRate<<" "<<curThroughputArr[0]<<" "<<curThroughputArr[1]<<" "<<jainIndex<<std::endl;
    // congestion<<bottleneckDataRateInt<<" "<<packetLossRate<<" "<<curThroughputArr[0]<<" "<<curThroughputArr[1]<<" "<<jainIndex<<std::endl;
       

    Simulator::Destroy ();


    return 0;
}


