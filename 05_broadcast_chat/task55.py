#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import socket

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) #несколько приложений могут слушать сокет
s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1) #пакеты будут широковещательные
s.bind(('',11720))

import sys
import select

input = [ sys.stdin, s ]

while True:
    r,w,e = select.select(input, [], [])
    for op in r:
        if (op == sys.stdin):
            #print (op.readline().rstrip())
            message = op.readline().rstrip()
            message = message.encode('utf-8')
            s.sendto(message,('172.20.10.15', 11720))
        else:
              data, addr = s.recvfrom(1024)  
              data = data.decode('utf-8')
              print("received message: ", data)

s.close()
