package org.interledger.ilp.ledger.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

import javax.money.Monetary;

import org.apache.commons.cli.HelpFormatter;
import org.interledger.ilp.core.ledger.events.LedgerEvent;
import org.interledger.ilp.ledger.client.commands.LedgerCommand;
import org.interledger.ilp.ledger.client.events.ClientLedgerConnectEvent;
import org.interledger.ilp.ledger.client.events.ClientLedgerErrorEvent;
import org.interledger.ilp.ledger.client.events.ClientLedgerMessageEvent;
import org.interledger.ilp.ledger.client.events.ClientLedgerTransferEvent;
import org.interledger.ilp.ledger.client.json.JsonMessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@SpringBootApplication
public class Application implements CommandLineRunner, ApplicationContextAware{
  
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private ApplicationContext applicationContext;
    
  public static void main(String[] args) {
    SpringApplication.run("classpath:/META-INF/application-context.xml", args);
  }

  public void run(String... args) throws Exception {
    
    if(args.length == 0) {
      
      Map<String, LedgerCommand> commands = applicationContext.getBeansOfType(LedgerCommand.class);

      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("<command> [options]", commands.get("helpCommand").getDefaultOptions());
      
       System.out.println("\r\n"
          + "Settings are read from application.properties but may be overridden through options.\r\n"
          + "\r\n"
          + "Commands:\r\n");
       
       for (LedgerCommand command : commands.values()) {
         System.out.println(command.getCommand() + " - " + command.getDescription());
       }
             
      Set<String> currencies = Monetary.getCurrencyProviderNames();
      for (String currency : currencies) {
        System.out.println(currency);
      }
       
      //Loop and read in commands
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      String commandLine = reader.readLine();
      
      while(!"quit".equals(commandLine.trim())) {
        
      args = commandLine.split(" ");
        
        if(args.length > 0) {
          LedgerCommand command = LedgerCommand.getLedgerCommand(args[0], commands);
          if(command != null) {
            try {
              command.run(args);
            } catch (Exception e) {
              e.printStackTrace(System.err);
            }
          } else {
            log.error("Unrecognized command: " + args[0]);
          }
        } else {
          //Empty line
        }
        
        commandLine = reader.readLine();
        
      }
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @EventListener
  public void onLedgerEvent(LedgerEvent event) {
    
    if(event instanceof ClientLedgerTransferEvent) {
      log.info("Transfer: {}", ((ClientLedgerTransferEvent) event).getTransfer().toString());
    }
    
    if (event instanceof ClientLedgerMessageEvent) {
      
      byte[] messageData = ((ClientLedgerMessageEvent) event).getMessage().getData();
      
      log.info("Message: {}", ((ClientLedgerMessageEvent) event).getMessage().toString());
      log.info("Message data: {}", new String(messageData, Charset.forName("UTF-8")));

        try {
          ObjectMapper mapper = new ObjectMapper();
          JsonMessageEnvelope innerMessage = mapper.readValue(messageData, JsonMessageEnvelope.class);
          log.info("Message received: {}", innerMessage);
        } catch (JsonParseException e) {
          e.printStackTrace();
        } catch (JsonMappingException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }
    
    if(event instanceof ClientLedgerErrorEvent) {
      log.error("Error event...", ((ClientLedgerErrorEvent) event).getError());
    }
    
    if(event instanceof ClientLedgerConnectEvent) {
      log.info("Connected...");
    }
    
  }
  
}
