package com.narkang.chry;

import com.narkang.chry.Request;
import com.narkang.chry.Response;

interface MyEventBusService {
   Response send(in Request request);
}
