#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>

#define RECIPIENT_PORT 10000
#define SIZE 1000
#define SMALL_SIZE 200

typedef struct {
  unsigned int length;
  unsigned char data[SIZE];
} Message;

typedef enum {
  OK, /* operation successful */
  BAD, /* unrecoverable error */
} Status;

typedef struct {
  enum {Request, Reply} messageType;    /* same size as an unsigned int */
  unsigned int RPCId;                   /* unique identifier */
  unsigned int procedureId;             /* e.g.(1,2,3,4) for (+, -, *, /) */
  int arg1;                             /* argument/return parameter */
  int arg2;                             /* argument/return parameter */
} RPCMessage;                           /* each int (and unsigned int)is 4 bytes */

typedef struct sockaddr_in SocketAddress;


Status GetRequest(Message *callMessage, int serverSocket, SocketAddress *clientSA);
Status SendReply(Message *replyMessage, int serverSocket, SocketAddress *clientSA);

struct hostent *gethostbyname();
void printSA(struct sockaddr_in sa) ;
void makeDestSA(struct sockaddr_in *sa, char *hostname, int port);
void makeReceiverSA(struct sockaddr_in *sa, int port);


/* main for sender and receiver - to send give s machine messag1 and message2
 - to receive give r
 */
void main(int argc,char **argv)
{
  SocketAddress localSA;
  int port = RECIPIENT_PORT;
  int serverSocketFd;
  Message requestMsg;
  
  makeReceiverSA(&localSA, port);
  if ((serverSocketFd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
    perror("socket failed");
  }
  if (bind(serverSocketFd, (struct sockaddr *)&localSA, sizeof(SocketAddress)) != 0) {
    perror("Bind failed");
  }
  for (;;) {
    GetRequest(&requestMsg, serverSocketFd, &localSA);
  }
  
  close(serverSocketFd);
}

Status GetRequest(Message *callMessage, int serverSocket, SocketAddress *clientSA) {
  int aLength, n;
  int i;
  
  aLength = sizeof(clientSA);
  if ((n = recvfrom(serverSocket, callMessage->data, SMALL_SIZE, 0, (struct sockaddr *)&clientSA, &aLength)) < 0)
    perror("Receive 1");
  else {
    callMessage->data[n] = '\0';
    printf("Received Message:(%s), length = %d\n", callMessage->data, n);
    callMessage->length = n;
    SendReply(callMessage, serverSocket, clientSA);
  }
}

Status SendReply(Message *replyMessage, int serverSocket, SocketAddress *clientSA) {
  int n;
  printf("SendReply: (%s), length: %d\n", replyMessage->data, replyMessage->length);
  if ((n = sendto(serverSocket, replyMessage->data, replyMessage->length, 0,
             (struct sockaddr *)&clientSA,
             sizeof(SocketAddress))) < 0)
    perror("Send failed");
  if(n != strlen(replyMessage->data)) printf("sent %d characters\n", n);
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

void setupSockets()
{
  
}

/*print a socket address */
void printSA(struct sockaddr_in sa)
{
  char mybuf[80];
  const char *ptr=inet_ntop(AF_INET, &sa.sin_addr, mybuf, 80);
  printf("sa = %d, %s, %d\n", sa.sin_family, mybuf, ntohs(sa.sin_port));
}
