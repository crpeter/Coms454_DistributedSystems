#include "RPC.h"


/* main for sender and receiver - to send give s machine messag1 and message2
 - to receive give r
 */
void main(int argc,char **argv)
{
  //  Hostname: pyrite-n1.cs.iastate.edu
  //  Host IP: 10.27.19.111
  if (argc <= 1) {
    printf("Usage: <hostname>\n");
    exit(1);
  }
  
  Message marshalledMsg, reply;
  char input[250];
  int clientSocket;
  SocketAddress serverSA, localSA;
  
  // Init SocketAddress for server and client
  makeLocalSA(&localSA, CLIENT_PORT);
  if (strcmp("local", argv[1]) == 0) makeLocalSA(&serverSA, RECIPIENT_PORT);
  else makeDestSA(&serverSA, argv[1], RECIPIENT_PORT);
  
  if ((clientSocket = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
    perror("socket failed");
    exit(-1);
  }
  if (bind(clientSocket, (struct sockaddr *)&localSA, sizeof(SocketAddress)) != 0){
    perror("Bind failed");
    exit(-1);
  }
  
  char op;
  int illegalOp = 0, clientMessageID = 1;
  for (;;) {
    printf("enter message to send to %s: ", argv[1]);
    scanf("%s", &input);
    if (strcmp("quit", input) == 0) break;
    if (strcmp("Stop", input) == 0 || strcmp("stop", input) == 0) {
      marshalledMsg.data[0] = STOP;
      marshalledMsg.data[1] = htonl(++clientMessageID);
      marshalledMsg.length = 2 * sizeof(int);
      DoOperation(&marshalledMsg, &reply, clientSocket, serverSA);
      goto endFor;
    }
    if (strcmp("Ping", input) == 0 || strcmp("ping", input) == 0) {
      marshalledMsg.data[0] = PING;
      marshalledMsg.data[1] = htonl(++clientMessageID);
      marshalledMsg.length = 2 * sizeof(int);
      DoOperation(&marshalledMsg, &reply, clientSocket, serverSA);
      goto endFor;
    }
    RPCMessage msg;
    sscanf(input, "%d %c %d", &msg.arg1, &op, &msg.arg2);
    if (msg.arg1 < 0 || msg.arg2 < 0) {
      printf("Enter non-negative numbers\n");
      goto endFor;
    }
    printf("scanned: %d %c %d\n", msg.arg1, op, msg.arg2);
    switch (op) {
      case '+':
        msg.procedureId = 0;
        break;
      case '-':
        msg.procedureId = 1;
        break;
      case '*':
        msg.procedureId = 2;
        break;
      case '/':
        msg.procedureId = 3;
        break;
      default:
        printf("Invalid op: %c", op);
    }
    // Must be less than 256 to fit in marshalled array block
    msg.RPCId = ++clientMessageID;
    msg.messageType = Request;
    if (!illegalOp) {
      marshall(&msg, &marshalledMsg);
      marshalledMsg.length = sizeof(RPCMessage);
      DoOperation(&marshalledMsg, &reply, clientSocket, serverSA);
    }
  endFor:
    illegalOp = 0;
  }
  
  close(clientSocket);
}

/**
 * unmarshals msg into marshalledMsg
 * in: msg
 * out marshalledMsg
 */
void marshall(RPCMessage *msg, Message *marshalledMsg) {
  //htons()/htonl()
  unsigned int newData[5];
  newData[0] = htonl(msg->messageType);
  newData[1] = htonl(msg->RPCId);
  newData[2] = htonl(msg->procedureId);
  newData[3] = htonl(msg->arg1);
  newData[4] = htonl(msg->arg2);
  memcpy(marshalledMsg->data, newData, sizeof(RPCMessage));
}

/**
 * unmarshals marshalledMsg into an RPCMessage
 * in: marshalledMsg
 * out msg
 */
void unmarshall(RPCMessage *msg, Message *marshalledMsg) {
  msg->messageType = ntohl(marshalledMsg->data[0]);
  msg->RPCId = ntohl(marshalledMsg->data[1]);
  msg->procedureId = ntohl(marshalledMsg->data[2]);
  msg->arg1 = ntohl(marshalledMsg->data[3]);
  msg->arg2 = ntohl(marshalledMsg->data[4]);
}

/**
 * Send message and wait for reply, using clientSocket and server address=serverSA
 * in: message, reply, clientSocket, serverSA
 */
Status DoOperation(Message *message, Message *reply, int clientSocket, SocketAddress serverSA) {
  int n;
  Status request;
  
  UDPsend(clientSocket, message, &serverSA);
  
  // Save status in request
  request = UDPreceive(clientSocket, reply, &serverSA);
  
//  int sckN, counter = 0;
//  while ((sckN = anyThingThere(clientSocket)) == 0) {
//    counter++;
//    // Resend UDPsend
//    UDPsend(clientSocket, message, &serverSA);
//    if (counter == 3) break;
//  }
  
  if (request == OK) {
    RPCMessage replyRPC;
    unmarshall(&replyRPC, reply);
    printf("received id: (%d), result: %d\n", replyRPC.RPCId, replyRPC.arg1);
  } else if (request == PING) {
    if (ntohl(reply->data[0]) == OK) printf("OK\n");
    else printf("BAD\n");
  } else if (request == DIVZERO) {
    printf("Divide by 0 Error\n");
  }
  else {
    perror("Receive 1");
  }
  
  return OK;
}

Status UDPsend(int socket, Message *m, SocketAddress *destination) {
  int n;
  // Number of bytes sent stored in n
  if ((n = sendto(socket, m->data, m->length, 0,
                  (struct sockaddr *)destination,
                  sizeof(SocketAddress))) < 0)
    perror("Send failed");
  if(n != m->length) printf("sent %d characters\n", n);
  return OK;
}

Status UDPreceive(int socket, Message *m, SocketAddress *origin) {
  int n, aLength;
  aLength = sizeof(origin);
  // n = number of bytes received from clientSA
  n = recvfrom(socket, m->data, SMALL_SIZE, 0, (struct sockaddr *)origin, &aLength);
  if (n < 0) {
    perror("Client recvfrom error");
    return BAD;
  } else if (n == 2 * sizeof(int)) {
    // if n is a single int, a ping or stop has been sent
    return PING;
  } else if (n == sizeof(int)) {
    return DIVZERO;
  } else {
    // Request message has been received
    // Ensure response is of type Reply
    if (ntohl(m->data[0]) != Reply) return BAD;
    m->length = n;
    m->data[n] = '\0';
    return OK;
  }
}

/**
 * make a socket address for a destination whose machine and port are given as arguments
 * in: hostname, port
 * out: sockaddr_in
 */
void makeDestSA(struct sockaddr_in *sa, char *hostname, int port)
{
  struct hostent *host;
  sa->sin_family = AF_INET;
  if ((host = gethostbyname(hostname)) == NULL) {
    printf("Unknown host name\n");
    exit(-1);
  }
  sa->sin_addr = *(struct in_addr *) (host->h_addr);
  sa->sin_port = htons(port);
}

/* make a socket address using any of the addressses of this computer
 for a local socket on any port */
void makeLocalSA(struct sockaddr_in *sa, port)
{
  sa->sin_family = AF_INET;
  sa->sin_port = htons(port);
  sa->sin_addr.s_addr = htonl(INADDR_ANY);
}

/*print a socket address */
void printSA(struct sockaddr_in sa)
{
  char mybuf[80];
  const char *ptr=inet_ntop(AF_INET, &sa.sin_addr, mybuf, 80);
  printf("sa = %d, %s, %d\n", sa.sin_family, mybuf, ntohs(sa.sin_port));
}

#include <sys/time.h>
/* use select to test whether there is any input on descriptor s*/
int anyThingThere(int clientSocket)
{
  unsigned long read_mask;
  struct timeval timeout;
  int n;
  
  // Code based on example on https://www.gnu.org/software/libc/manual/html_node/Waiting-for-I_002fO.html
  fd_set set;
  
  /* Initialize the file descriptor set. */
  FD_ZERO (&set);
  FD_SET (clientSocket, &set);
  
  timeout.tv_sec = 1; /*seconds wait*/
  timeout.tv_usec = 0; /* micro seconds*/
  
  /* select returns 0 if timeout, 1 if input available, -1 if error. */
  n = (select (FD_SETSIZE, &set, NULL, NULL, &timeout));
  return n;
}
