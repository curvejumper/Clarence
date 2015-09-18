
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author curvejumper
 */
public class ChatServer {
    private static final String USAGE = "Usage: java ChatServer";
    
    private GpioPinDigitalOutput pin0;
    private GpioPinDigitalOutput pin1;

    /** Default port number on which this server to be run. */
    private static final int PORT_NUMBER = 1992;

    private void prepareGPIO() {
        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();
        
        // provision gpio pin #01 as an output pin and turn on
        pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "MyOutlet", PinState.LOW);
        pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "MyOutlet", PinState.LOW);

        // set shutdown state for this pin
        pin0.setShutdownOptions(true, PinState.LOW);
        pin1.setShutdownOptions(true, PinState.LOW);

	//set the pins off(Supposedly)
	pin0.low();
	pin1.low();
		
        System.out.println("--> GPIO state should be: OFF..but its ON lol");
    }

    /** List of print writers associated with current clients,
     * one for each. */
    private List<PrintWriter> clients;

    /** Creates a new server. */
    public ChatServer() {
        clients = new LinkedList<PrintWriter>();
    }

    /** Starts the server. */
    public void start() {
        System.out.println("AndyChat server started on port "
                           + PORT_NUMBER + "!"); 
        prepareGPIO();
        try {
            ServerSocket s = new ServerSocket(PORT_NUMBER); 
            for (;;) {
                Socket incoming = s.accept(); 
                new ClientHandler(incoming).start(); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("AndyChat server stopped."); 
    }

    /** Adds a new client identified by the given print writer. */
    private void addClient(PrintWriter out) {
        synchronized(clients) {
            clients.add(out);
        }
    }

    /** Adds the client with given print writer. */
    private void removeClient(PrintWriter out) {
        synchronized(clients) {
            clients.remove(out);
        }
    }

    /** Broadcasts the given text to all clients. */
    private void broadcast(String msg) {
        for (PrintWriter out: clients) {
            out.println(msg);
            out.flush();
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            System.out.println(USAGE);
            System.exit(-1);
        }
        new ChatServer().start();
    }

    /** A thread to serve a client. This class receive messages from a
     * client and broadcasts them to all clients including the message
     * sender. */
    private class ClientHandler extends Thread {

        /** Socket to read client messages. */
        private Socket incoming; 

        /** Creates a hander to serve the client on the given socket. */
        public ClientHandler(Socket incoming) {
            this.incoming = incoming;
        }

        /** Starts receiving and broadcasting messages. */
        public void run() {
            PrintWriter out = null;
            try {
                out = new PrintWriter(
                        new OutputStreamWriter(incoming.getOutputStream()));
                
                // inform the server of this new client
                ChatServer.this.addClient(out);

                out.print("Welcome to AndyChat! ");
                out.println("Enter BYE to exit."); 
                out.flush();

                BufferedReader in 
                    = new BufferedReader(
                        new InputStreamReader(incoming.getInputStream())); 
                for (;;) {
                    String msg = in.readLine(); 
                    if (msg == null) {
                        break; 
                    } else {
                        if (msg.trim().equals("BYE")) 
                            break; 
                        System.out.println("Received: " + msg);
                        //Do stuff on the raspberryPi with message
                        raspPiCmd(msg);
                        // broadcast the receive message
                        ChatServer.this.broadcast(msg);
                    }
                }
                incoming.close(); 
                ChatServer.this.removeClient(out);
            } catch (Exception e) {
                if (out != null) {
                    ChatServer.this.removeClient(out);
                }
                e.printStackTrace(); 
            }
        }

        private void raspPiCmd(String msg) {
            switch(msg){
                case "enteredRegion":
                    //toggle pins to output signal
                    pin0.low();
                    pin1.low();
                    break;
                case "leftRegion":
                    //toggle pins to not output signal
                    pin0.high();
                    pin1.high();
                    break;
				case "outlet_1_on":
					//turn pin 0 on
					pin0.low();
					break;
				case "outlet_1_off":
					//turn pin 0 off
					pin0.high();
					break;
				case "outlet_2_on":
					//turn pin 1 on
					pin1.low();
					break;
				case "outlet_2_off":
					//turn pin 1 off
					pin1.high();
					break;
                default:
                    //do nothing
                    break;
        }
    }
    }
}
