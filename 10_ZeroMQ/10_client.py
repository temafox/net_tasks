import zmq

context = zmq.Context()
socket = context.socket(zmq.REQ)
socket.connect("tcp://127.0.0.1:10000")

socket.connect("tcp://127.0.0.1:10001")

socket.connect("tcp://127.0.0.1:10002")

print("Client\n")
while True:
    msg = input()
    socket.send_string(msg)
    print("Received: ", msg)
    socket.recv_string()
