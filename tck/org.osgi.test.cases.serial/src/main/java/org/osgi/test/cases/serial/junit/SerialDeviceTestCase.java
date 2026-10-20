/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/

package org.osgi.test.cases.serial.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.serial.SerialDevice;
import org.osgi.service.serial.SerialEvent;
import org.osgi.service.serial.SerialEventListener;
import org.osgi.service.serial.SerialPortConfiguration;
import org.osgi.test.cases.serial.util.SerialTestProxy;
import org.osgi.test.cases.serial.util.TestSerialEventListener;
import org.osgi.test.cases.serial.util.TestServiceListener;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.tck.step.TestStepProxy;

public class SerialDeviceTestCase {

	@InjectBundleContext
	BundleContext		context;
	private SerialTestProxy	testProxy;

	@BeforeEach
	public void setUp() throws Exception {
		this.testProxy = new SerialTestProxy(new TestStepProxy(context));
	}

	@AfterEach
	public void tearDown() throws Exception {
		this.testProxy.close();
	}

	/**
	 * Tests service register/unregister. Please set in advance the information
	 * of your device to the system property (bnd.bnd file).
	 */
	@Test
	public void testSerialDevice01() {
		try {
			String filter = "(" + Constants.OBJECTCLASS + "=" + SerialDevice.class.getName() + ")";
			TestServiceListener<SerialDevice> regListener = new TestServiceListener<>(
					ServiceEvent.REGISTERED);
			TestServiceListener<SerialDevice> unregListener = new TestServiceListener<>(
					ServiceEvent.UNREGISTERING);
			context.addServiceListener(regListener, filter);
			context.addServiceListener(unregListener, filter);

			String message = "[TEST-SD01] Connect a device.";
			String[] ids = testProxy.executeTestStep("add", message, new String[] {});

			assertThat(regListener.size()).as("Only one registered service event is expected.").isOne();

			ServiceReference<SerialDevice> ref = regListener.get(0);
			Long serviceId1 = (Long) ref.getProperty(Constants.SERVICE_ID);

			message = "Disconnect the device.";
			testProxy.executeTestStep("remove", message, new String[] {ids[0]});

			assertThat(unregListener.size()).as("Only one unregistering service event is expected.").isOne();
			ref = unregListener.get(0);
			assertThat(ref.getProperty(Constants.SERVICE_ID)).as("The unregisterd service is not correct.").isEqualTo(serviceId1);

			context.removeServiceListener(regListener);
			context.removeServiceListener(unregListener);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage(), e);
		}
	}

	/**
	 * Tests services register/unregister.
	 */
	@Test
	public void testSerialDevice02() {
		try {
			String filter = "(" + Constants.OBJECTCLASS + "=" + SerialDevice.class.getName() + ")";
			TestServiceListener<SerialDevice> regListener = new TestServiceListener<>(
					ServiceEvent.REGISTERED);
			TestServiceListener<SerialDevice> unregListener = new TestServiceListener<>(
					ServiceEvent.UNREGISTERING);
			context.addServiceListener(regListener, filter);
			context.addServiceListener(unregListener, filter);

			String message = "[TEST-SD02] Connect the first device.";
			String[] ids1 = testProxy.executeTestStep("add", message, new String[] {});

			assertThat(regListener.size()).as("Only one registered service event is expected.").isOne();

			ServiceReference<SerialDevice> ref = regListener.get(0);
			Long serviceId1 = (Long) ref.getProperty(Constants.SERVICE_ID);

			message = "Connect the second device.";
			String[] ids2 = testProxy.executeTestStep("add", message, new String[] {});

			assertThat(regListener.size()).as("Only two registered service events are expected.").isEqualTo(2);

			ref = regListener.get(1);
			Long serviceId2 = (Long) ref.getProperty(Constants.SERVICE_ID);

			message = "Disconnect the first device.";
			testProxy.executeTestStep("remove", message, new String[] {ids1[0]});

			assertThat(unregListener.size()).as("Only one unregistering service event is expected.").isOne();

			ref = unregListener.get(0);
			assertThat(ref.getProperty(Constants.SERVICE_ID)).as("The unregisterd service is not correct.").isEqualTo(serviceId1);

			message = "Disconnect the second device.";
			testProxy.executeTestStep("remove", message, new String[] {ids2[0]});

			assertThat(unregListener.size()).as("Only two unregistering service events are expected.").isEqualTo(2);

			ref = unregListener.get(1);
			assertThat(ref.getProperty(Constants.SERVICE_ID)).as("The unregisterd service is not correct.").isEqualTo(serviceId2);

			context.removeServiceListener(regListener);
			context.removeServiceListener(unregListener);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage(), e);
		}
	}

	/**
	 * Tests SerialDevice service properties.
	 */
	@Test
	public void testSerialDevice03() {
		String[] ids = null;
		try {
			TestServiceListener<SerialDevice> listener = new TestServiceListener<>(
					ServiceEvent.REGISTERED);
			context.addServiceListener(listener, "(" + Constants.OBJECTCLASS + "=" + SerialDevice.class.getName() + ")");

			String message = "[TEST-SD03] Connect a device.";
			ids = testProxy.executeTestStep("add", message, new String[] {});

			context.removeServiceListener(listener);

			ServiceReference<SerialDevice> ref = listener.get(0);
			String[] category = (String[]) ref.getProperty(org.osgi.service.device.Constants.DEVICE_CATEGORY);
			boolean categoryFlag = false;
			for (int i = 0; i < category.length; i++) {
				if (category[i].equals(SerialDevice.DEVICE_CATEGORY)) {
					categoryFlag = true;
					break;
				}
			}
			assertThat(categoryFlag).as("The following service property is not correct: " + org.osgi.service.device.Constants.DEVICE_CATEGORY).isTrue();

			Object comport = ref.getProperty(SerialDevice.SERIAL_COMPORT);
			assertThat(comport instanceof String).as("The following service property is not correct: " + SerialDevice.SERIAL_COMPORT).isTrue();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage(), e);
		} finally {
			if (ids != null) {
				testProxy.executeTestStep("remove", "Disconnect the remaining device.", new String[] {ids[0]});
			}
		}
	}

	/**
	 * Tests SerialDevice configuration.
	 */
	@Test
	public void testSerialDevice04() {
		String[] ids = null;
		try {
			TestServiceListener<SerialDevice> listener = new TestServiceListener<>(
					ServiceEvent.REGISTERED);
			context.addServiceListener(listener, "(" + Constants.OBJECTCLASS + "=" + SerialDevice.class.getName() + ")");

			String message = "[TEST-SD04] Connect a device.";
			ids = testProxy.executeTestStep("add", message, new String[] {});

			context.removeServiceListener(listener);
			SerialDevice serialDevice = context.getService(listener.get(0));

			SerialPortConfiguration config = serialDevice.getConfiguration();
			config.getBaudRate();
			config.getDataBits();
			config.getFlowControl();
			config.getParity();
			config.getStopBits();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage(), e);
		} finally {
			if (ids != null) {
				testProxy.executeTestStep("remove", "Disconnect the remaining device.", new String[] {ids[0]});
			}
		}
	}

	/**
	 * Tests Serial event.
	 */
	@Test
	public void testSerialDevice05() {
		String[] ids1 = null;
		String[] ids2 = null;
		try {
			String message = "[TEST-SD05] Connect the first device.";
			ids1 = testProxy.executeTestStep("add", message, new String[] {});

			message = "Connect the second device.";
			ids2 = testProxy.executeTestStep("add", message, new String[] {});

			TestSerialEventListener serialListenerAll = new TestSerialEventListener();
			context.registerService(SerialEventListener.class.getName(), serialListenerAll, null);

			Collection<ServiceReference<SerialDevice>> refs1 = context
					.getServiceReferences(SerialDevice.class,
							"(service.id=" + ids1[0] + ")");
			String comport1 = (String) refs1.iterator()
					.next()
					.getProperty(SerialDevice.SERIAL_COMPORT);
			TestSerialEventListener serialListener1 = new TestSerialEventListener();
			Dictionary<String,Object> props1 = new Hashtable<>();
			props1.put(SerialEventListener.SERIAL_COMPORT, comport1);
			context.registerService(SerialEventListener.class.getName(), serialListener1, props1);

			Collection<ServiceReference<SerialDevice>> refs2 = context
					.getServiceReferences(SerialDevice.class,
							"(service.id=" + ids2[0] + ")");
			String comport2 = (String) refs2.iterator()
					.next()
					.getProperty(SerialDevice.SERIAL_COMPORT);
			TestSerialEventListener serialListener2 = new TestSerialEventListener();
			Dictionary<String,Object> props2 = new Hashtable<>();
			props2.put(SerialEventListener.SERIAL_COMPORT, comport2);
			context.registerService(SerialEventListener.class.getName(), serialListener2, props2);

			message = "Send data from the first device.";
			testProxy.executeTestStep("event", message, new String[] {ids1[0]});

			assertThat(serialListenerAll.isReceived()).as("It is expected that the SerialListener does not have serial.comport receive all events.").isTrue();
			assertThat(serialListenerAll.getReceivedComPort()).as("The ComPort name of the event is not correct.").isEqualTo(comport1);
			assertThat(serialListenerAll.getReceivedType()).as("The type of the event is not correct.").isEqualTo(SerialEvent.DATA_AVAILABLE);

			assertThat(serialListener1.isReceived()).as("It is expected that the SerialListener has serial.comport receive events from the port.").isTrue();
			assertThat(serialListener1.getReceivedComPort()).as("The ComPort name of the event is not correct.").isEqualTo(comport1);
			assertThat(serialListener1.getReceivedType()).as("The type of the event is not correct.").isEqualTo(SerialEvent.DATA_AVAILABLE);

			assertThat(serialListener2.isReceived()).as("It is expected that the SerialListener has serial.comport receive events from only the port.").isFalse();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage(), e);
		} finally {
			if (ids1 != null) {
				testProxy.executeTestStep("remove", "Disconnect the remaining device.", new String[] {ids1[0]});
			}
			if (ids2 != null) {
				testProxy.executeTestStep("remove", "Disconnect the remaining device.", new String[] {ids2[0]});
			}
		}
	}
}
