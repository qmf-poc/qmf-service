## QMF PoC Service

### Build & run

```
sbt assemby
java -jar target/scala-3.6.4/service-assembly-0.1.0-SNAPSHOT.jar
```

builds fat jar: target/scala-3.6.4/service-assembly-0.1.0-SNAPSHOT.jar


### Components

```mermaid
flowchart TB
    subgraph "QMF Objects Storage"
        QMFObjectsStorage[Lucene QMF Objects Storage]
    end
    subgraph "JsonRPC"
        pendingRequestsPromises
    end
    subgraph Agent
        AgentsRegistry
    end
    subgraph "to Agent Transport"
        pendingRequestsPromises --> OutgoingMessageHandler
        outgoingQueue[Outgoing Queue]
        outgoingQueue --> OutgoingMessageHandler[OutgoingMessageHandlerLive]
        OutgoingMessageIdGenerator
    end
    subgraph "from Agent Transport"
        QMFObjectsStorage --> IncomingMessageHandler
        pendingRequestsPromises --> IncomingMessageHandler
        IncomingMessageHandler[IncomingMessageHandlerLive]
        OutgoingMessagesStorage
    end
    subgraph Routes
        pingAgent[ping-agent]
        OutgoingMessageHandler --> pingAgent
        OutgoingMessageIdGenerator --> pingAgent
        OutgoingMessageHandler --> catalog
        OutgoingMessageIdGenerator --> catalog
        OutgoingMessageHandler --> run
        OutgoingMessageIdGenerator --> run
        OutgoingMessagesStorage --> agent
        IncomingMessageHandler --> agent
        AgentsRegistry --> agent
        QMFObjectsStorage --> query
        QMFObjectsStorage --> get
        ping --> routes
        pingAgent --> routes
        catalog --> routes
        run --> routes
        agent --> routes
        query --> routes
        get --> routes
    end
    subgraph "ZIO Server"
        NettyConfig
        httpConfig
        routes
        NettyConfig --> zioServer
        httpConfig --> zioServer
        routes --> zioServer
    end
    subgraph "qmf Server"
        zioServer --> qmfHttpServer
    end
    subgraph " "
        qmfHttpServer --> program
    end
```