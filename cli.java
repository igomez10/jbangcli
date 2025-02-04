///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.mongodb:mongodb-driver-sync:4.9.1

import static java.lang.System.*;

import java.time.LocalDateTime;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;

public class cli {
    public static void main(String... args) {
        // Create a CodecRegistry that can handle the Human class
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder()
                .register(Human.class) // Explicitly register Human class
                .build();

        CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(pojoCodecProvider));

        // Configure MongoDB client with the codec registry
        MongoClientSettings settings = MongoClientSettings.builder()
                .codecRegistry(pojoCodecRegistry)
                .build();

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            MongoDatabase db = mongoClient.getDatabase("humans");
            MongoCollection<Human> collection = db.getCollection("humans", Human.class);

            Human newHuman = new Human("Ignacio", 22, LocalDateTime.now());
            out.println(newHuman);
            InsertOneResult res = collection.insertOne(newHuman);
            out.println("Inserted document with id: " + res.getInsertedId());
        }
    }

    // Make Human class static and public
    public static class Human {
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
            return "Human{name='" + name + "', age=" + age + ", dateOfBirth=" + dateOfBirth + "}";
        }
    }
}
