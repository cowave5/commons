/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools;/*
 * Copyright (c) 2017～2099 Cowave All Rights Reserved.
 *
 * For licensing information, please contact: https://www.cowave.com.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 */
import com.cowave.commons.tools.ssh.output.LoggerExtOutputStream;
import com.cowave.commons.tools.ssh.output.LoggerOutputStream;
import com.cowave.commons.tools.ssh.Terminal;

import java.io.IOException;

/**
 * @author jiangbo
 */
public class TerminalTest {

    public static void main(String[] args) throws IOException {
        Terminal.Result result = Terminal.execRemote("10.64.4.96", 22, "root", "cowave", "ping 10.64.4.96 -c 5;1+1;echo 'aa'");

        Terminal.Result result2 = Terminal.execRemote("10.64.4.96", 22, "root", "cowave", "ping 10.64.4.96 -c 5;1+1;echo 'aa'",
                LoggerOutputStream.apply(), LoggerExtOutputStream.apply());
    }
}
