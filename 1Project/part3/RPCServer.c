#include "RPC.h"

/* main for sender and receiver - to send give s machine messag1 and message2
 - to receive give r
 */
void main(int argc,char **argv)
{
  SocketAddress localSA, clientSA;
  int serverSocketFd;
  Message requestMsg;
  
  makeLocalSA(&localSA, RECIPIENT_PORT);
  makeReceiverSA(&clientSA, CLIENT_PORT);
  if ((serverSocketFd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
    perror("Server Socket Failed");
  }
  if (bind(serverSocketFd, (struct sockaddr *)&localSA, sizeof(SocketAddress)) != 0) {
    perror("Bind to Socket Failed");
  }
  for (;;) {
    if (GetRequest(&requestMsg, serverSocketFd, &clientSA) == BAD) {
      break;
    }
  }
  
  close(serverSocketFd);
}

/**
 * unmarshals marshalledMsg into an RPCMessage
 * in: marshalledMsg
 * out msg
 */
void unmarshall(RPCMessage *msg, Message *marshalledMsg) {
  // Set fields of msg from marshalled network message
  msg->messageType = ntohl(marshalledMsg->data[0]);
  msg->RPCId = ntohl(marshalledMsg->data[1]);
  msg->procedureId = ntohl(marshalledMsg->data[2]);
  msg->arg1 = ntohl(marshalledMsg->data[3]);
  msg->arg2 = ntohl(marshalledMsg->data[4]);
}

Status getResult(RPCMessage *msg, int *result) {
  // Perform operation based on procedureId, +,-,*,/
  switch (msg->procedureId) {
    case 0:
      add(msg->arg1, msg->arg2, result);
      break;
    case 1:
      subtract(msg->arg1, msg->arg2, result);
      break;
    case 2:
      multiply(msg->arg1, msg->arg2, result);
      break;
    case 3:
      if (divide(msg->arg1, msg->arg2, result) == DIVZERO) return DIVZERO;
      break;
    default:
      break;
  }
  return OK;
}

Status add( int x, int y, int *result) {
  *result = x + y;
  return OK;
}
Status subtract( int x, int y, int *result) {
  *result = x - y;
  return OK;
}
Status multiply( int x, int y, int *result) {
  *result = x * y;
  return OK;
}
Status divide( int x, int y, int *result) {
  if (y == 0) return DIVZERO;
  *result = x / y;
  return OK;
}

/**
 * Expect a request from client socket address.
 * Once a message is received, unmarshall the message and perform the operation requested.
 * in: callMessage, reply, serverSocket, clientSA
 */
Status GetRequest(Message *callMessage, int serverSocket, SocketAddress *clientSA) {
  int aLength;
  Status request;

  aLength = sizeof(clientSA);
  // n = number of bytes received from clientSA
  if ((request = UDPreceive(serverSocket, callMessage, clientSA)) == OK) {
    RPCMessage msg;
    // unmarshall function unmarshalls msg and performs op on args and returns result
    unmarshall(&msg, callMessage);
    // Get result and return div zero for arithmatic error
    int result;
    if (getResult(&msg, &result) == DIVZERO) {
      callMessage->data[0] = htonl(DIVZERO);
      callMessage->length = sizeof(int);
      SendReply(callMessage, serverSocket, clientSA);
      return OK;
    }
    // print pID and args received
    printf("Received Message: id = %d, op = %d, arg1 = %d, arg2 = %d\n", msg.RPCId, msg.procedureId, msg.arg1, msg.arg2);
    // store result in arg1 spot of RPCMessage AKA i=3 in marshalled message
    callMessage->data[3] = htonl(result);
    // set messageType of response to Reply
    callMessage->data[0] = htonl(Reply);
    callMessage->length = sizeof(RPCMessage);
    SendReply(callMessage, serverSocket, clientSA);
  } else if (request == PING) {
    callMessage->data[0] = htonl(OK);
    callMessage->length = 2 * sizeof(int);

    SendReply(callMessage, serverSocket, clientSA);
    return PING;
  } else if (request == STOP) {
    callMessage->data[0] = htonl(OK);
    callMessage->length = 2 * sizeof(int);
    
    SendReply(callMessage, serverSocket, clientSA);
    // loop that calls GetRequest breaks if a BAD is returned
    return BAD;
  }
  return OK;
}

/**
 * Send reply using serverSocket and destination address and clientSA
 * in: replyMessage, serverSocket, clientSA
 */
Status SendReply(Message *replyMessage, int serverSocket, SocketAddress *clientSA) {
  if (UDPsend(serverSocket, replyMessage, clientSA) == OK) return OK;
  
  return BAD;
}

Status UDPsend(int socket, Message *m, SocketAddress *destination) {
  int n;
  printf("SendReply: (%d), (%d), length: %d\n", ntohl(m->data[0]), ntohl(m->data[1]), m->length);
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
  n = recvfrom(socket, m->data, SMALL_SIZE, 0, (struct sockaddr *)origin, &aLength);
  
  // n = number of bytes received from clientSA
  if (n < 0) {
    perror("Server recvfrom error");
    return BAD;
  } else if (n == 2 * sizeof(int)) {
    // if n is a single int, a ping or stop has been sent
    if (m->data[0] == STOP) {
      return STOP;
    }
    return PING;
  } else {
    // Request message has been received
    // If type is not request return bad
    if (ntohl(m->data[0]) != Request) return BAD;
    m->length = n;
    m->data[n] = '\0';
    return OK;
  }
}

/* make a socket address for a destination whose machine and port
 are given as arguments */
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

void makeReceiverSA(struct sockaddr_in *sa, int port)
{
  sa->sin_family = AF_INET;
  sa->sin_port = htons(port);
  sa->sin_addr.s_addr = htonl(INADDR_ANY);
}

void makeLocalSA(struct sockaddr_in *sa, int port)
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

