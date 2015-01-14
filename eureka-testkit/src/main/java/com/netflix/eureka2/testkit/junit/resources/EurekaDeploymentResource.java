package com.netflix.eureka2.testkit.junit.resources;

import com.netflix.eureka2.client.Eureka;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.resolver.WriteServerResolverSet;
import com.netflix.eureka2.testkit.embedded.EurekaDeployment;
import com.netflix.eureka2.testkit.embedded.EurekaDeployment.EurekaDeploymentBuilder;
import com.netflix.eureka2.testkit.embedded.server.EmbeddedReadServer;
import com.netflix.eureka2.testkit.embedded.server.EmbeddedWriteServer;
import com.netflix.eureka2.transport.EurekaTransports.Codec;
import org.junit.rules.ExternalResource;

/**
 * @author Tomasz Bak
 */
public class EurekaDeploymentResource extends ExternalResource {

    private final int writeClusterSize;
    private final int readClusterSize;
    private final Codec codec;

    private EurekaDeployment eurekaDeployment;

    public EurekaDeploymentResource(int writeClusterSize, int readClusterSize) {
        this(writeClusterSize, readClusterSize, Codec.Avro);
    }

    public EurekaDeploymentResource(int writeClusterSize, int readClusterSize, Codec codec) {
        this.writeClusterSize = writeClusterSize;
        this.readClusterSize = readClusterSize;
        this.codec = codec;
    }

    public EurekaDeployment getEurekaDeployment() {
        return eurekaDeployment;
    }

    /**
     * Create {@link EurekaClient} instance connected to a particular write server.
     *
     * @param idx id of a write server where to connect
     */
    public EurekaClient connectToWriteServer(int idx) {
        EmbeddedWriteServer server = eurekaDeployment.getWriteCluster().getServer(idx);
        return Eureka.newClientBuilder(
                server.getDiscoveryResolver(),
                server.getRegistrationResolver()
        ).withCodec(codec).build();
    }

    /**
     * Create {@link EurekaClient} instance connected to a particular read server (interest subscription only).
     *
     * @param idx id of a read server where to connect
     */
    public EurekaClient connectToReadServer(int idx) {
        EmbeddedReadServer server = eurekaDeployment.getReadCluster().getServer(idx);
        return Eureka.newClientBuilder(
                server.getDiscoveryResolver(),
                null
        ).withCodec(codec).build();
    }

    /**
     * Create {@link EurekaClient} instance connected to a write cluster.
     */
    public EurekaClient connectToWriteCluster() {
        return Eureka.newClientBuilder(
                eurekaDeployment.getWriteCluster().discoveryResolver(),
                eurekaDeployment.getWriteCluster().registrationResolver()
        ).withCodec(codec).build();
    }

    /**
     * Create {@link EurekaClient} instance connected to a read cluster.
     */
    public EurekaClient connectToReadCluster() {
        return Eureka.newClientBuilder(
                eurekaDeployment.getReadCluster().discoveryResolver(),
                null
        ).withCodec(codec).build();
    }

    /**
     * Create {@link EurekaClient} in canonical setup, where read cluster is discovered from
     * write cluster first.
     */
    public EurekaClient connectToEureka() {
        return Eureka.newClientBuilder(
                WriteServerResolverSet.forResolvers(
                        eurekaDeployment.getWriteCluster().registrationResolver(),
                        eurekaDeployment.getWriteCluster().discoveryResolver()
                ),
                eurekaDeployment.getReadCluster().getVip()
        ).withCodec(codec).build();
    }

    @Override
    protected void before() throws Throwable {
        eurekaDeployment = new EurekaDeploymentBuilder()
                .withWriteClusterSize(writeClusterSize)
                .withReadClusterSize(readClusterSize)
                .withEphemeralPorts(true)
                .withCodec(codec)
                .build();
    }

    @Override
    protected void after() {
        if (eurekaDeployment != null) {
            eurekaDeployment.shutdown();
        }
    }
}