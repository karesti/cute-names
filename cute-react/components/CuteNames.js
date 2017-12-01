import React from 'react'
import EventBus from 'vertx3-eventbus-client'

const CuteNames = () => {
    const eventBus = new EventBus("http://localhost:8082/eventbus");
    eventBus.enableReconnect(true);
    var name = '';
    eventBus.onOpen = function () {
        console.error("register");
        eventBus.registerHandler('cute-names', function (error, message) {
            if (error === null) {
                console.error(message.body);
                name = message.body;
            } else {
                console.error(error, 'cute-names');
            }
        });
    };
  return (
    <div className='cute'>
      <h1>Cute name</h1>
      <p>{name}</p>
    </div>
  )
}
export default CuteNames
