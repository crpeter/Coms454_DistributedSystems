#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>


#define CLIENT_PORT 10001
#define RECIPIENT_PORT 10044
#define SIZE 1000
#define SMALL_SIZE 200

typedef struct {
  unsigned int length;
  unsigned int data[SIZE];
} Message;

typedef enum {
  OK, /* operation successful */
  BAD, /* unrecoverable error */
  PING, /* ping from the client, return OK */
  STOP, /* stop from the client, return OK and exit */
  DIVZERO /* client attempted to divide by 0 */
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
Status DoOperation(Message *message, Message *reply, int clientSocket, SocketAddress serverSA);

Status UDPsend(int socket, Message *m, SocketAddress *destination);
Status UDPreceive(int socket, Message *m, SocketAddress *origin);

Status getResult(RPCMessage *msg, int *result);
Status add( int x, int y, int *result);
Status subtract( int x, int y, int *result);
Status multiply( int x, int y, int *result);
Status divide( int x, int y, int *result);

struct hostent *gethostbyname();
void marshall(RPCMessage *msg, Message *marshalledMsg);
void unmarshall(RPCMessage *msg, Message *marshalledMsg);
void printSA(struct sockaddr_in sa);
void makeDestSA(struct sockaddr_in *sa, char *hostname, int port);
void makeReceiverSA(struct sockaddr_in *sa, int port);
void makeLocalSA(struct sockaddr_in *sa, int port);
