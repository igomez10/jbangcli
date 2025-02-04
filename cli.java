///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.mongodb:mongodb-driver-sync:4.9.1
//DEPS info.picocli:picocli:4.2.0
//DEPS org.slf4j:slf4j-api:1.7.30
//DEPS org.slf4j:slf4j-simple:1.7.30

import static java.lang.System.*;

import java.time.LocalDateTime;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Command(name = "cli", mixinStandardHelpOptions = true, version = "cli 1.0", description = "CLI to interact with MongoDB", subcommands = {
        cli.AddCommand.class, cli.ListCommand.class, cli.DeleteAllCommand.class, cli.DeleteByIdCommand.class })
public class cli implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(cli.class);

    public static void main(String... args) {
        // Set the logging level to ERROR to silence other logs
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
        System.setProperty(org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX + "org.mongodb.driver", "ERROR");
        int exitCode = new CommandLine(new cli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        // Default command behavior
        CommandLine.usage(this, out);
    }

    @Command(name = "add", description = "Add a new human to the database")
    static class AddCommand implements Runnable {
        @Option(names = { "-n", "--name" }, description = "Name of the human", required = true)
        private String name;

        @Option(names = { "-a", "--age" }, description = "Age of the human", required = true)
        private Integer age;

        @Override
        public void run() {
            try (MongoClient mongoClient = HumanProvider.getMongoClient()) {
                MongoDatabase db = mongoClient.getDatabase("humans");
                MongoCollection<Human> collection = db.getCollection("humans", Human.class);

                Human newHuman = new Human(name, age, LocalDateTime.now());
                out.println(newHuman);
                InsertOneResult res = collection.insertOne(newHuman);
                out.println("Inserted document with id: " + res.getInsertedId());
            }
        }
    }

    @Command(name = "list", description = "List all humans in the database")
    static class ListCommand implements Runnable {
        @Override
        public void run() {
            try (MongoClient mongoClient = HumanProvider.getMongoClient()) {
                MongoDatabase db = mongoClient.getDatabase("humans");
                MongoCollection<Human> collection = db.getCollection("humans", Human.class);

                List<Human> humans = collection.find().into(new ArrayList<>());
                humans.forEach(out::println);
            }
        }
    }

    @Command(name = "deleteAll", description = "Delete all humans in the database")
    static class DeleteAllCommand implements Runnable {
        @Override
        public void run() {
            try (MongoClient mongoClient = HumanProvider.getMongoClient()) {
                MongoDatabase db = mongoClient.getDatabase("humans");
                MongoCollection<Human> collection = db.getCollection("humans", Human.class);

                collection.deleteMany(new org.bson.Document());
                out.println("All humans have been deleted from the database.");
            }
        }
    }

    @Command(name = "deleteById", description = "Delete a human by ID")
    static class DeleteByIdCommand implements Runnable {
        @Option(names = { "-i", "--id" }, description = "ID of the human", required = true)
        private String id;

        @Override
        public void run() {
            try (MongoClient mongoClient = HumanProvider.getMongoClient()) {
                MongoDatabase db = mongoClient.getDatabase("humans");
                MongoCollection<Human> collection = db.getCollection("humans", Human.class);

                DeleteResult result = collection.deleteOne(new org.bson.Document("_id", new ObjectId(id)));
                if (result.getDeletedCount() > 0) {
                    out.println("Human with ID " + id + " has been deleted.");
                } else {
                    out.println("No human found with ID " + id);
                }
            }
        }
    }

    // Make Human class static and public
    public static class Human {
        private ObjectId id;
        private String name;
        private Integer age;
        private LocalDateTime dateOfBirth;

        // Default constructor
        public Human() {
        }

        public Human(
                String name,
                Integer age,
                LocalDateTime dateOfBirth) {
            this.name = name;
            this.age = age;
            this.dateOfBirth = dateOfBirth;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        // Standard getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public LocalDateTime getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(LocalDateTime dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        @Override
        public String toString() {
            return "Human{id='" + id + "', name='" + name + "', age=" + age + ", dateOfBirth=" + dateOfBirth + "}";
        }
    }
}

class HumanProvider {
    private static final CodecProvider pojoCodecProvider = PojoCodecProvider.builder()
            .register(cli.Human.class) // Explicitly register Human class
            .build();

    private static final CodecRegistry pojoCodecRegistry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(pojoCodecProvider));

    private static final MongoClientSettings settings = MongoClientSettings.builder()
            .codecRegistry(pojoCodecRegistry)
            .build();

    public static MongoClient getMongoClient() {
        return MongoClients.create(settings);
    }
}
