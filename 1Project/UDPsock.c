#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>

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

struct hostent *gethostbyname() ;
void printSA(struct sockaddr_in sa) ;
void makeDestSA(struct sockaddr_in * sa, char *hostname, int port) ;
void makeLocalSA(struct sockaddr_in *sa) ;
void receiver(int port) ;
void sender(char *message1, char *machine, int port);

#define RECIPIENT_PORT 10000
#define SIZE 1000

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
    printf("Usage:s(end) ...or r(eceive) ??\n");
    exit(1);
  }
  if (*argv[1]  == 's') {
    if (argc <= 1) {
      printf("Usage: s machine\n");
      exit(1);
    }
    char message[256];
    while (strcmp("quit", message) != 0) {
      printf("enter message to send to %s: ", argv[2]);
      scanf("%s", &message);
      
      sender(message, argv[2], port);
    }
  } else if (*argv[1]  == 'r') {
    receiver(port);
  }
  else printf("send machine or receive??\n");
}

/*print a socket address */
void printSA(struct sockaddr_in sa)
{
	char mybuf[80];
	const char *ptr=inet_ntop(AF_INET, &sa.sin_addr, mybuf, 80);
	printf("sa = %d, %s, %d\n", sa.sin_family, mybuf, ntohs(sa.sin_port));
}

/* make a socket address for a destination whose machine and port
	are given as arguments */
void makeDestSA(struct sockaddr_in * sa, char *hostname, int port)
{
	struct hostent *host;

	sa->sin_family = AF_INET;
	if ((host = gethostbyname(hostname)) == NULL) {
		printf("Unknown host name\n");
		exit(-1);
	}
	sa-> sin_addr = *(struct in_addr *) (host->h_addr);
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

/* make a socket address using any of the addressses of this computer
for a local socket on given port */
void makeReceiverSA(struct sockaddr_in *sa, int port)
{
	sa->sin_family = AF_INET;
	sa->sin_port = htons(port);
	sa->sin_addr.s_addr = htonl(INADDR_ANY);
}

/*receive two messages via s new socket,
	print out the messages received and close the socket
	bind to  any of the addresses of this computer
	using port given as argument */
void receiver(int port)
{
  char message1[SIZE];
	struct sockaddr_in mySocketAddress, aSocketAddress;
	int s,aLength, n;
	int i;

	if((s = socket(AF_INET, SOCK_DGRAM, 0))<0) {
		perror("socket failed");
		return;
	}
	makeReceiverSA(&mySocketAddress, port);

	if( bind(s, (struct sockaddr *)&mySocketAddress, sizeof(struct sockaddr_in))!= 0){
		perror("Bind failed\n");
		close(s);
		return;
	}

	printSA(mySocketAddress);
	aLength = sizeof(aSocketAddress);
	aSocketAddress.sin_family = AF_INET;
	if((n = recvfrom(s, message1,  SIZE, 0, (struct sockaddr *)&aSocketAddress, &aLength))<0)
		perror("Receive 1") ;
	else{
		printSA(aSocketAddress);
		message1[n]='\0';
		printf("\nReceived Message:(%s), length = %d \n",
			message1,n);
	}
	close(s);
}


/*do send after receive ready, open socket
	bind socket to local internet port
		use any of the local computer's addresses
	send two messages with given lengths to machine and  port
	close socket
*/

void sender(char *message1, char *machine, int port)
{
	int s, n;
	char message[SIZE];
	struct sockaddr_in mySocketAddress, yourSocketAddress;

	if(( s = socket(AF_INET, SOCK_DGRAM, 0))<0) {
		perror("socket failed");
		return;
	}

	makeDestSA(&yourSocketAddress,machine, port);
	printSA(yourSocketAddress);
  strcpy(message,message1);
  if( (n = sendto(s, message, strlen(message), 0, (struct sockaddr *)&yourSocketAddress,
    sizeof(struct sockaddr_in))) < 0)
    perror("Send failed\n");
  if(n != strlen(message)) printf("sent %d\n",n);
  
  close(s);
}

#include <sys/time.h>
/* use select to test whether there is any input on descriptor s*/
int anyThingThere(int s)
{
	unsigned long read_mask;
	struct timeval timeout;
	int n;

	timeout.tv_sec = 10; /*seconds wait*/
	timeout.tv_usec = 0; /* micro seconds*/
	read_mask = (1<<s);
	if((n = select(32, (fd_set *)&read_mask, 0, 0, &timeout))<0)
		perror("Select fail:\n");
	else printf("n = %d\n");
	return n;
}




