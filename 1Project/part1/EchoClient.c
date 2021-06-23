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

typedef struct {
  unsigned int length;
  unsigned char data[SIZE];
} Message;

typedef enum {
  OK, /* operation successful */
  BAD, /* unrecoverable error */
} Status;

typedef struct sockaddr_in SocketAddress;

Status DoOperation(Message *message, Message *reply, int socket, SocketAddress serverSA);
Status GetRequest(Message *callMessage, int socket, SocketAddress *clientSA);
Status SendReply(Message *replyMessage, int socket, SocketAddress clientSA);

struct hostent *gethostbyname();
void printSA(struct sockaddr_in sa) ;
void makeDestSA(struct sockaddr_in * sa, char *hostname, int port);
void makeLocalSA(struct sockaddr_in *sa);


/* main for sender and receiver - to send give s machine messag1 and message2
 - to receive give r
 */
void main(int argc,char **argv)
{
  char hostbuffer[256];
  char *IPbuffer;
  struct hostent *host_entry;
  int hostname;
  
  //  Hostname: pyrite-n1.cs.iastate.edu
  //  Host IP: 10.27.19.111
  int port = RECIPIENT_PORT;
  
  if (argc <= 1) {
    printf("Usage: <hostname>\n");
    exit(1);
  }
  
  Message message, reply;
  char input[250];
  int clientSocket;
  SocketAddress serverSA, localSA;
  
  makeLocalSA(&localSA);
  makeDestSA(&serverSA, argv[1], port);
  if ((clientSocket = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
    perror("socket failed");
    exit(-1);
  }
  if (bind(clientSocket, (struct sockaddr *)&localSA, sizeof(SocketAddress)) != 0){
    perror("Bind failed");
    exit(-1);
  }
  
  for (;;) {
    printf("enter message to send to %s: ", argv[1]);
    scanf("%s", input);
    if (strcmp("quit", input) == 0) break;
    strcpy(message.data, input);
    message.length = strlen(message.data);
    
    DoOperation(&message, &reply, clientSocket, serverSA);
  }
  
  close(clientSocket);
}

Status DoOperation(Message *message, Message *reply, int clientSocket, SocketAddress serverSA) {
  int n;
  
  printf("Sending Message: (%s), length: %d \n", message->data, message->length);
  if ((n = sendto(clientSocket, message->data, message->length, 0,
             (struct sockaddr *)&serverSA,
             sizeof(struct sockaddr_in))) < 0)
    perror("Send failed");
  if(n != strlen(message->data)) printf("sent %d characters\n", n);
  

  int aLength = sizeof(serverSA);
  if ((n = recvfrom(clientSocket, reply->data,  SIZE, 0,
                    (struct sockaddr *)&serverSA, &aLength)) < 0)
    perror("Receive 1");
  else {
    reply->data[n]='\0';
    printf("Received Message:(%s), length: %d \n", reply->data, n);
  }
  
  return OK;
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

/* make a socket address using any of the addressses of this computer
 for a local socket on any port */
void makeLocalSA(struct sockaddr_in *sa)
{
  sa->sin_family = AF_INET;
  sa->sin_port = htons(0);
  sa->sin_addr.s_addr = htonl(INADDR_ANY);
}

/*print a socket address */
void printSA(struct sockaddr_in sa)
{
  char mybuf[80];
  const char *ptr=inet_ntop(AF_INET, &sa.sin_addr, mybuf, 80);
  printf("sa = %d, %s, %d\n", sa.sin_family, mybuf, ntohs(sa.sin_port));
}
