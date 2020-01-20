#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Dec  9 22:50:45 2019

@author: tema
"""

import urllib.request
import time

from sleekxmpp import ClientXMPP

username = 'temafox@xabber.org'
passwd = '0q1u0a1n2t0u0m'
to = 'ianaantonova@jabber.ru'
client = ClientXMPP(username, passwd)
client.connect()
client.process()

while True:

        req = urllib.request.Request('http://weather.nsu.ru/weather.xml')
        response = urllib.request.urlopen(req)
        page = response.read()
        str_page = str(page)
        index1 = str_page.find('current')
        index2 = str_page.find('/current')
        data = str_page[index1+8:index2-1]
        client.send_message(to, 'Current temperature: ' + data + '°C')
        time.sleep(1)

client.disconnect()
