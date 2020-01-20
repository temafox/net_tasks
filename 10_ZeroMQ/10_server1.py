import zmq

context = zmq.Context()
socket = context.socket(zmq.REP)
socket.bind("tcp://127.0.0.1:10000")

print("The 1st server\n")
while True:
    msg = socket.recv_string()
    print("You've sent : ", msg)

    socket.send_string(msg)
