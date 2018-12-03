package no.nav.modiaeventdistribution.kafka;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@Value
@Builder
public class KafkaConsumerConfig {

    @NotEmpty
    public String topicName;
    @NotEmpty
    public String groupId;

    @Builder.Default
    public List<BootstrapServer> bootstrapServers = new ArrayList<>();
    @NotEmpty
    public String username;
    @NotEmpty
    public String password;
    @NotNull
    public BiConsumer<String, String> handler;

    @Value
    @Builder
    public static class BootstrapServer {
        @NotEmpty
        public String host;
        @NotEmpty
        public int port;

        public static List<BootstrapServer> parse(String configString) {
            return stream(configString.split(",")).map(s -> {
                String[] hostAndPort = s.split(":");
                return BootstrapServer.builder()
                        .host(hostAndPort[0])
                        .port(Integer.parseInt(hostAndPort[1]))
                        .build();
            }).collect(toList());
        }
    }
}
