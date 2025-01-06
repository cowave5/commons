/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.lambda.meta;

import java.io.*;

/**
 * 当前类是 {@link java.lang.invoke.SerializedLambda } 的一个镜像
 *
 * <p>Create by hcl at 2020/7/17
 *
 * @author jiangbo
 */
public class SerializedMeta implements Serializable {
    private Class<?> capturingClass;
    private String implMethodName;
    private String instantiatedMethodType;

    public static SerializedMeta extract(Serializable serializable) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(serializable);
            oos.flush();
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    Class<?> clazz = super.resolveClass(desc);
                    return clazz == java.lang.invoke.SerializedLambda.class ? SerializedMeta.class : clazz;
                }

            }) {
                return (SerializedMeta) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getInstantiatedMethodType() {
        return instantiatedMethodType;
    }

    public Class<?> getCapturingClass() {
        return capturingClass;
    }

    public String getImplMethodName() {
        return implMethodName;
    }
}
