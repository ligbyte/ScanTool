/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hello;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SerialPortFinder {

    public List<String> getDrivers() {
        File[] files = new File("/dev").listFiles();
        if (files == null) return null;
        List<String> ttyDevices = new ArrayList<>();
        for (File file : files) {
            if (file.getName().startsWith("tty")) ttyDevices.add(file.getAbsolutePath());
        }
        return ttyDevices;
    }

    public String getAcmDevicesPath() {
        for (String driver : getDrivers()) {
            if (driver.contains("ttyACM")) {
                return driver;
            }
        }
        return "";
    }
}
