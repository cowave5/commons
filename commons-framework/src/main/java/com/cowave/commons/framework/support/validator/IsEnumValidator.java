/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 *
 * @author shanhuiming
 *
 */
public class IsEnumValidator implements ConstraintValidator<IsEnum, CharSequence> {

    private boolean ignoreCase;

    private Enum<?>[] enumValues;

    @Override
    public void initialize(IsEnum annotation) {
        this.ignoreCase = annotation.ignoreCase();
        this.enumValues = annotation.value().getEnumConstants();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        for (Enum<?> enumValue : enumValues) {
            if (ignoreCase ? enumValue.name().equalsIgnoreCase(value.toString()) : enumValue.name().equals(value.toString())) {
                return true;
            }
        }
        return false;
    }
}
