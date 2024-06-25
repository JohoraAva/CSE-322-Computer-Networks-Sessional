#include "tcp-adaptive-reno.h"
#include "ns3/log.h"
#include "ns3/simulator.h"
#include "rtt-estimator.h"
#include "tcp-socket-base.h"
#include "ns3/point-to-point-dumbbell.h"

NS_LOG_COMPONENT_DEFINE("TcpAdaptiveReno");
using namespace std;

namespace ns3
{

NS_OBJECT_ENSURE_REGISTERED(TcpAdaptiveReno);

TypeId
TcpAdaptiveReno::GetTypeId()
{
    static TypeId tid =
        TypeId("ns3::TcpAdaptiveReno")
            .SetParent<TcpNewReno>()
            .SetGroupName("Internet")
            .AddConstructor<TcpAdaptiveReno>()
            .AddAttribute(
                "FilterType",
                "Use this to choose no filter or Tustin's approximation filter",
                EnumValue(TcpAdaptiveReno::TUSTIN),
                MakeEnumAccessor(&TcpAdaptiveReno::m_fType),
                MakeEnumChecker(TcpAdaptiveReno::NONE, "None", TcpAdaptiveReno::TUSTIN, "Tustin"))
            .AddTraceSource("EstimatedBW",
                            "The estimated bandwidth",
                            MakeTraceSourceAccessor(&TcpAdaptiveReno::m_currentBW),
                            "ns3::TracedValueCallback::Double");
    return tid;
}

TcpAdaptiveReno::TcpAdaptiveReno()
    : TcpWestwoodPlus(),
    //time variables
    m_minRtt(Time(0)),
    m_curRtt(Time(0)),
    m_jPacketLossRtt(Time(0)),
    m_curConjRtt(Time(0)),
    m_lastConRtt(Time(0)),

    //window variables
    m_incWnd(0),
    m_baseWnd(0),
    m_probeWnd(0)
{
    NS_LOG_FUNCTION(this);
}

TcpAdaptiveReno::TcpAdaptiveReno(const TcpAdaptiveReno& sock)
    : TcpWestwoodPlus(sock),
      //time variables
    m_minRtt(Time(0)),
    m_curRtt(Time(0)),
    m_jPacketLossRtt(Time(0)),
    m_curConjRtt(Time(0)),
    m_lastConRtt(Time(0)),

    //window variables
    m_incWnd(0),
    m_baseWnd(0),
    m_probeWnd(0)
{
    NS_LOG_FUNCTION(this);
    NS_LOG_LOGIC("Invoked the copy constructor");
}

TcpAdaptiveReno::~TcpAdaptiveReno()
{
}

//acknowledged packets
void
TcpAdaptiveReno::PktsAcked(Ptr<TcpSocketState> tcb, uint32_t packetsAcked, const Time& rtt)
{
    NS_LOG_FUNCTION(this << tcb << packetsAcked << rtt);

    if (rtt.IsZero())
    {
        NS_LOG_WARN("RTT measured is zero!");
        return;
    }

    m_ackedSegments += packetsAcked;


    //calculate min rtt
    m_minRtt= (m_minRtt.IsZero()==0 ? rtt : std::min(rtt,m_minRtt));

    m_curRtt = rtt;

    TcpWestwoodPlus::EstimateBW(rtt, tcb);
}

double
TcpAdaptiveReno::EstimateCongestionLevel()
{
    //calculate rtt

    float smoothing_factor=0.85;

    if(m_lastConRtt< m_minRtt)
        smoothing_factor=0.0;
    double conjRtt=smoothing_factor*m_lastConRtt.GetSeconds()+(1-smoothing_factor)*m_jPacketLossRtt.GetSeconds();
    m_curConjRtt=Seconds(conjRtt);


    return (std::min((m_curRtt.GetSeconds()-m_minRtt.GetSeconds())/(conjRtt-m_minRtt.GetSeconds()),1.0));
}

void 
TcpAdaptiveReno::EstimateIncWnd(Ptr<TcpSocketState> tcb)
{
    //calculate incwnd
    double conjestion=EstimateCongestionLevel();
    int m=1000;
    double maxSegSize=tcb->m_segmentSize* tcb->m_segmentSize;

    TcpWestwoodPlus::EstimateBW(m_curRtt, tcb);
    DataRate bw=  m_currentBW.Get();
    double intTerm=1/(m*maxSegSize);
    double maxIncWnd=  (bw*intTerm).GetBitRate();

    double alpha=10;
    intTerm=(1/alpha)-((1/alpha+1)/std::exp(alpha));
    double beta=2*maxIncWnd*intTerm;
    intTerm=(1/alpha)-((1/alpha+0.5)/std::exp(alpha));
    double gamma=1-2*maxIncWnd*intTerm;

    m_incWnd=(int) ((maxIncWnd/std::exp(alpha*conjestion))+(beta*conjestion+gamma));
}


void
TcpAdaptiveReno::CongestionAvoidance (Ptr<TcpSocketState> tcb, uint32_t segmentsAcked)
{
    NS_LOG_FUNCTION (this << tcb << segmentsAcked);

    if(segmentsAcked>0)
    {
        EstimateIncWnd(tcb);
        double incVal=(double) (tcb->m_segmentSize*tcb->m_segmentSize)/tcb->m_cWnd.Get();
        incVal=std::max(incVal,1.0);
        m_baseWnd+=incVal;

        if(m_probeWnd+m_incWnd/tcb->m_cWnd.Get()>0)
        {
            m_probeWnd-=m_probeWnd+m_incWnd/tcb->m_cWnd.Get();
        }
        else
        {
            m_probeWnd=0.0;
        }
        // m_probeWnd=max(m_probeWnd+m_incWnd/tcb->m_cWnd.Get(),0.0);

        tcb->m_cWnd=m_baseWnd+m_probeWnd;
    }
}

uint32_t
TcpAdaptiveReno::GetSsThresh(Ptr<const TcpSocketState> tcb, uint32_t bytesInFlight [[maybe_unused]])
{
    
    m_lastConRtt=m_curConjRtt;
    m_jPacketLossRtt=m_curRtt;


    double congestion=EstimateCongestionLevel();

    uint32_t ssThresh=std::max(2*tcb->m_segmentSize,(uint32_t) (tcb->m_cWnd/(1.0+congestion)));

    m_baseWnd=ssThresh;
    m_probeWnd=0;

    return ssThresh;
}

Ptr<TcpCongestionOps>
TcpAdaptiveReno::Fork()
{
    return CreateObject<TcpAdaptiveReno>(*this);
}

} // namespace ns3
