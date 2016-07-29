package pl.epodr.sk.messaging;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class RoutingKeyTest {

	@Test
	public void testRoutingKey() {
		RoutingKey rt = new RoutingKey("rt.production.womi.modified");
		assertEquals("rt.production", rt.getSender());
		assertEquals("womi.modified", rt.getRequest());

		rt = new RoutingKey("rt.production.blaaa");
		assertEquals("rt.production", rt.getSender());
		assertEquals("blaaa", rt.getRequest());

		try {
			new RoutingKey("rt.productionblaaa");
			Assert.fail();
		} catch (IllegalArgumentException e) {
		}
	}

}
