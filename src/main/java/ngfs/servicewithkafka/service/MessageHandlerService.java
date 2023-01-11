package ngfs.servicewithkafka.service;

import ngfs.servicewithkafka.model.HousePayload;

import javax.validation.Valid;

public interface MessageHandlerService {

    String notifyHouseChanges(@Valid HousePayload payload, Integer key, int partition, long timestamp);

}
