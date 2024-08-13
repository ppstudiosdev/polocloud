package dev.httpmarco.polocloud.node.events;

import dev.httpmarco.polocloud.api.event.Event;
import dev.httpmarco.polocloud.api.event.EventFactory;
import dev.httpmarco.polocloud.api.event.EventPoolRegister;
import dev.httpmarco.polocloud.api.event.EventProvider;
import dev.httpmarco.polocloud.api.packet.resources.event.EventCallPacket;
import dev.httpmarco.polocloud.api.packet.resources.event.EventSubscribePacket;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.services.ClusterLocalServiceImpl;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
@Getter
@Accessors(fluent = true)
public final class EventProviderImpl implements EventProvider {

    private final EventFactory factory = new EventFactoryImpl();

    public EventProviderImpl() {

        Node.instance().clusterProvider().localNode().transmit().listen(EventCallPacket.class, (transmit, packet) -> {
            if(Node.instance().serviceProvider().isServiceChannel(transmit)){
                factory.call(packet.event());
                return;
            }
            for (var pool : EventPoolRegister.pools()) {
                pool.acceptActor(packet.event());
            }
        });

        Node.instance().clusterProvider().localNode().transmit().listen(EventSubscribePacket.class, (transmit, packet) -> {
            var service = Node.instance().serviceProvider().find(transmit);

            if (service instanceof ClusterLocalServiceImpl localService) {
                // register a binding on the packet for transmit the event as packet to the service
                localService.eventSubscribePool().subscribe(packet.packetClass(), event -> localService.transmit().sendPacket(new EventCallPacket(event)));
            } else {
                log.warn("Service try to subscribe the event {} but only local service can do this!", packet.getClass());
                log.warn("Break subscription of the event.");
            }
        });
    }

    @Override
    public <T extends Event> void listen(Class<T> eventClazz, Consumer<T> event) {

    }
}
