#!/bin/bash


# speed vs throughput
file="scratch/1905022_1.cc"
datFile1="scratch/ThroughputVsNodes.dat"
datFile2="scratch/PacketRatioVsNodes.dat"
# pngFile

totalNodes=$1
numFlow=$2
packetPerSec=$3
covArea=$4

nodeArray="20 40 60 80 100"
flowArray="10 20 30 40 50"
packetArray="100 200 300 400 500"
covArray="1 2 3 4 5"


if [ -f $datFile1 ]; then
    rm $datFile1
fi

if [ -f $datFile2 ]; then
    rm $datFile2
fi

for p in {1..4}; do

    if [ $p = 1 ] ;then
        pngFile1="scratch/ThroughputVsNodes1.png"
        pngFile2="scratch/PacketRatioVsNodes1.png"
        title1="Throughput vs Nodes"
        title2="Packet Ratio vs Nodes"
        touch $datFile1
        touch $datFile2

        for i in $nodeArray; do
            echo "Running simulation with node = $i"
            ./ns3 run "$file $i $numFlow $packetPerSec $covArea $p $datFile1 $datFile2" 
            echo "Simulation with node = $i completed"
        done

gnuplot -persist <<-EOFMarker
    set terminal png size 700,500
    set output "$pngFile1"
    plot "$datFile1" using 1:2 title "$title1" with linespoints
    set output "$pngFile2"
    plot "$datFile2" using 1:2 title "$title2" with linespoints
EOFMarker
    rm $datFile1
    rm $datFile2

    elif [ $p = 2 ] ;then
        pngFile1="scratch/ThroughputVsFlow1.png"
        pngFile2="scratch/PacketRatioVsFlow1.png"
        title1="Throughput vs Flow"
        title2="Packet Ratio vs Flow"
        touch $datFile1
        touch $datFile2

        for i in $flowArray; do
            echo "Running simulation with flow = $i"
            ./ns3 run "$file $totalNodes $i $packetPerSec $covArea $p $datFile1 $datFile2"
            echo "Simulation with flow = $i completed"
        done

gnuplot -persist <<-EOFMarker
    set terminal png size 700,500
    set output "$pngFile1"
    plot "$datFile1" using 1:2 title "$title1" with linespoints
    set output "$pngFile2"
    plot "$datFile2" using 1:2 title "$title2" with linespoints
EOFMarker
    rm $datFile1
    rm $datFile2

    elif [ $p = 3 ] ;then
        pngFile1="scratch/ThroughputVsPacket1.png"
        pngFile2="scratch/PacketRatioVsPacket1.png"
        title1="Throughput vs Packet"
        title2="Packet Ratio vs Packet"
        touch $datFile1
        touch $datFile2

        for i in $packetArray; do
            echo "Running simulation with packet = $i"
            ./ns3 run "$file $totalNodes $numFlow $i $covArea $p $datFile1 $datFile2"
            echo "Simulation with packet = $i completed"
        done

gnuplot -persist <<-EOFMarker
    set terminal png size 700,500
    set output "$pngFile1"
    plot "$datFile1" using 1:2 title "$title1" with linespoints
    set output "$pngFile2"
    plot "$datFile2" using 1:2 title "$title2" with linespoints
EOFMarker
    rm $datFile1
    rm $datFile2

    elif [ $p = 4 ] ;then
        pngFile1="scratch/ThroughputVsCoverageArea1.png"
        pngFile2="scratch/PacketRatioVsCoverageArea1.png"
        title1="Throughput vs Coverage Area"
        title2="Packet Ratio vs Coverage Area"
        touch $datFile1
        touch $datFile2


        for i in $covArray; do
            echo "Running simulation with coverage Area = $i"
            ./ns3 run "$file $totalNodes $numFlow $packetPerSec $i $p $datFile1 $datFile2"
            echo "Simulation with coverage Area = $i completed"
        done

gnuplot -persist <<-EOFMarker
    set terminal png size 700,500
    set output "$pngFile1"
    plot "$datFile1" using 1:2 title "$title1" with linespoints
    set output "$pngFile2"
    plot "$datFile2" using 1:2 title "$title2" with linespoints
EOFMarker
    rm $datFile1
    rm $datFile2
    fi
done 



