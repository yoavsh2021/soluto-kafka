import {ProtobufMessage, Server, loadPackageDefinition, ServerCredentials, credentials} from 'grpc';
import {loadSync} from '@grpc/proto-loader';

const PROTO_PATH = __dirname + '/message.proto';

export type Headers = {
    recordOffset: string;
    recordTimestamp: string;
    topic: string;
};

const packageDefinition = loadSync(PROTO_PATH, {
    keepCase: true,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true,
});
const ProtobufMessage = loadPackageDefinition(packageDefinition) as ProtobufMessage;

const _callTarget = run => async (call, callback) => {
    try {
        const receivedTimestamp = Date.now();
        const payload = JSON.parse(call.request.msgJson);
        await run({
            payload,
            headers: {
                recordOffset: parseInt(call.request.recordOffset) || -1,
                recordTimestamp: parseInt(call.request.recordTimestamp) || -1,
                topic: call.request.topic,
            },
        });
        callback(null, {statusCode: 200, receivedTimestamp, completedTimestamp: Date.now()});
    } catch (e) {
        callback(null, {statusCode: e.statusCode ?? e.status ?? 500});
    }
};

const getServer = execute => {
    const server = new Server();
    server.addService(ProtobufMessage.CallTarget.service, {
        callTarget: _callTarget(execute),
    });
    return server;
};

export const startServer = (port, execute) => {
    const routeServer = getServer(execute);
    routeServer.bind(`0.0.0.0:${port}`, ServerCredentials.createInsecure());
    routeServer.start();
    return routeServer;
};

type TargetResponse = {
    statusCode: number;
};

let _client;
export const createClient = url => {
    if (!_client) {
        _client = new ProtobufMessage.CallTarget(url, credentials.createInsecure());
    }
    return {
        callTarget: <T>(payload: T, recordOffset?: number,  customData: {[key: string]: string} = {}): Promise<TargetResponse> =>
            new Promise(resolve =>
                _client.callTarget({msgJson: JSON.stringify(payload), recordOffset, ...customData}, (_, responsePayload) =>
                    resolve(responsePayload)
                )
            ),
    };
};
