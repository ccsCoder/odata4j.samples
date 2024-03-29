package org.odata4j.heroku;

import org.odata4j.producer.inmemory.InMemoryProducer;
import java.util.Map.Entry;
import java.util.Properties;
import org.odata4j.producer.*;
import java.io.File;
import java.net.URL;
import org.core4j.*;
import org.odata4j.edm.EdmEntityType;


public class HerokuInMemoryProducerFactory implements ODataProducerFactory {
  
  @Override
  public ODataProducer create(Properties properties) {
    final InMemoryProducer producer = new InMemoryProducer("HerokuInMemory");

    // expose this jvm's thread information (Thread instances) as an entity-set called "Threads"
    producer.register(Thread.class, "Threads", new Func<Iterable<Thread>>() {
      public Iterable<Thread> apply() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null)
          tg = tg.getParent();
        Thread[] threads = new Thread[1000];
        int count = tg.enumerate(threads, true);
        return Enumerable.create(threads).take(count);
      }
    }, "Id");
    
    
    // expose all files in the current directory (File instances) as an entity-set called "Files"
    producer.register(File.class, "Files", new Func<Iterable<File>>() {
      public Iterable<File> apply() {
        return Enumerable.create(new File(".").listFiles());
        }
    }, "Name");

    // expose current system properties (Map.Entry instances) as an entity-set called "SystemProperties"
    producer.register(Entry.class, "SystemProperties", new Func<Iterable<Entry>>() {
      public Iterable<Entry> apply() { 
        return (Iterable<Entry>) (Object) System.getProperties().entrySet();
      }
    }, "Key");

    // expose current environment variables (Map.Entry instances) as an entity-set called "EnvironmentVariables"
    producer.register(Entry.class, "EnvironmentVariables", new Func<Iterable<Entry>>() {
      public Iterable<Entry> apply() {
        return (Iterable<Entry>) (Object) System.getenv().entrySet();
      }
    }, "Key");

    // expose this producer's entity-types (EdmEntityType instances) as an entity-set called "EdmEntityTypes"
    producer.register(EdmEntityType.class, "EdmEntityTypes", new Func<Iterable<EdmEntityType>>() {
      public Iterable<EdmEntityType> apply() {
        return producer.getMetadata().getEntityTypes();
      }
    }, "FQName");

    // expose a current listing of exchange traded funds sourced from an external csv (EtfInfo instances) as an entity-set called "ETFs"
    producer.register(EtfInfo.class, "ETFs", Funcs.wrap(new ThrowingFunc<Iterable<EtfInfo>>() {
      public Iterable<EtfInfo> apply() throws Exception {
        return getETFs();
      }
    }), "Symbol");

    // expose an large list of integers as an entity-set called "Integers"
    producer.register(Integer.class, Integer.class, "Integers", new Func<Iterable<Integer>>() {
      public Iterable<Integer> apply() {
        return Enumerable.range(0, Integer.MAX_VALUE);
      }
    }, Funcs.method(Integer.class, Integer.class, "intValue"));

    return producer;
  }

  private static Iterable<EtfInfo> getETFs() throws Exception {
    return Enumerables.lines(new URL("http://www.masterdata.com/HelpFiles/ETF_List_Downloads/AllETFs.csv")).select(new Func1<String, EtfInfo>() {
      public EtfInfo apply(String csvLine) {
        return EtfInfo.parse(csvLine);
      }
    }).skip(1); // skip header line
  }

  public static class EtfInfo {

    private final String name;
    private final String symbol;
    private final String fundType;

    private EtfInfo(String name, String symbol, String fundType) {
      this.name = name;
      this.symbol = symbol;
      this.fundType = fundType;
    }

    public static EtfInfo parse(String csvLine) {
      csvLine = csvLine.substring(0, csvLine.lastIndexOf(','));
      int i = csvLine.lastIndexOf(',');
      String type = csvLine.substring(i + 1);
      csvLine = csvLine.substring(0, csvLine.lastIndexOf(','));
      i = csvLine.lastIndexOf(',');
      String sym = csvLine.substring(i + 1);
      csvLine = csvLine.substring(0, csvLine.lastIndexOf(','));
      String name = csvLine;
      name = name.startsWith("\"") ? name.substring(1) : name;
      name = name.endsWith("\"") ? name.substring(0, name.length() - 1) : name;
      name = name.replace("\u00A0", " ");

      return new EtfInfo(name, sym, type);
    }

    public String getName() {
      return name;
    }

    public String getSymbol() {
      return symbol;
    }

    public String getFundType() {
      return fundType;
    }
  }
   
}
