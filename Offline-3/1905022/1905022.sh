# #!/bin/bash

file="scratch/1905022.cc"
datFile1="scratch/throughputvsdatarate.dat"
datFile2="scratch/throughputvslossrate.dat"
cwndFile1="scratch/output/output1.dat"
cwndFile2="scratch/output/output2.dat"
algo1="TcpNewReno"
algo2="TcpAdaptiveReno"


bottleneckDataRate=50
# bottleneckDataRateArr="1 2 3 5 10 20 50 100 150 200 250 300"
packetLossrate=6 #power of 10

if [ -f $datFile1 ]; then
    rm $datFile1
fi
if [ -f $datFile2 ]; then
    rm $datFile2
fi

if [ -f $cwndFile1 ]; then
    rm $cwndFile1
fi
if [ -f $cwndFile2 ]; then
    rm $cwndFile2
fi

# graph for data rate
touch $datFile1
for j in {1..10}; do
     echo "Running simulation with bottleneck data rate = $j"
    ./ns3 run "$file $j $packetLossrate $datFile1"
    echo "Simulation with bottleneck data rate = $j completed"
done

# # plot graph
pngFile="scratch/throughputvsDatarate.png"
title="Throughput vs Bootleneck Data Rate"
gnuplot -persist <<-EOFMarker
    set terminal png size 700,500
    set output "$pngFile"
    plot "$datFile1" using 1:3 title "$algo1" with linespoints, \
            "$datFile1" using 1:4 title "$algo2" with linespoints
EOFMarker


# # 
for i in {1..2}; do
    if [ $i = 1 ];then
        for j in {1..300};do
            if [ -f $cwndFile1 ]; then
                rm $cwndFile1
            fi
            if [ -f $cwndFile2 ]; then
                rm $cwndFile2
            fi
            touch $cwndFile1
            touch $cwndFile2
             echo "Running simulation with bottleneck data rate = $j"
            ./ns3 run "$file $j $packetLossrate $datFile1"
            echo "Simulation with bottleneck data rate = $j completed"

            gnuplot -persist <<-EOFMarker
            set terminal png size 700,500
            set output "scratch/graph/cwndvstime_datarate$j.png"
            plot "$cwndFile1" using 1:2 title "$algo1" with linespoints,\
            "$cwndFile2" using 1:2 title "$algo2" with linespoints
EOFMarker

        done
    elif [ $i = 2 ];then
        pngFile="scratch/throughputvsPacketLossRate.png"
        title="Throughput vs Packet Loss Rate"

        for j in {2..6};do
            if [ -f $cwndFile1 ]; then
                rm $cwndFile1
            fi
            if [ -f $cwndFile2 ]; then
                rm $cwndFile2
            fi
            touch $cwndFile1
            touch $cwndFile2
            echo "Running simulation with packet loss rate = $j"
            ./ns3 run "$file $bottleneckDataRate $j $datFile2 " 
            echo "Simulation with packet loss rate = $j completed"

            gnuplot -persist <<-EOFMarker
            set terminal png size 700,500
            set output "scratch/graph/cwndvstime_packetLossRate$j.png"
            plot "$cwndFile1" using 1:2 title "$algo1" with linespoints,\
            "$cwndFile2" using 1:2 title "$algo2" with linespoints
EOFMarker
        done
# plot graph for packet loss
gnuplot -persist <<-EOFMarker
    set terminal png size 700,500
    set output "$pngFile"
    plot "$datFile2" using 2:3 title "$algo1" with linespoints,\
            "$datFile2" using 2:4 title "$algo2" with linespoints
EOFMarker
    fi
done


# fairness index vs throughput,loss rate

pngFile1="scratch/FairnessIndexvsDataRate.png"
pngFile2="scratch/FairnessIndexvsPacketLossRate.png"
# title="Throughput vs Packet Loss Rate"
gnuplot -persist <<-EOFMarker
    set terminal png size 700,500
    set output "$pngFile1"
    plot "$datFile1" using 1:5 title "Fairness Index vs Bottleneck DataRate" with linespoints
    set output "$pngFile2"
    plot "$datFile2" using 2:5 title "Fairness Index vs Packet Loss Rate" with linespoints
EOFMarker













