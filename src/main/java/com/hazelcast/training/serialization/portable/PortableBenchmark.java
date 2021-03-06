/*
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
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
 *
 */

package com.hazelcast.training.serialization.portable;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import com.hazelcast.internal.serialization.SerializationServiceBuilder;
import com.hazelcast.internal.serialization.impl.DefaultSerializationServiceBuilder;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.serialization.SerializationService;
import com.hazelcast.training.serialization.benchmarks.ShoppingCartBenchmark;

import java.util.Date;
import java.util.Random;

public class PortableBenchmark implements ShoppingCartBenchmark {

    private HazelcastInstance hz;
    private IMap<Object, Object> cartMap;
    private int maxOrders = 100 * 1000;
    private int maxCartItems = 5;
    private String[] products;
    private final boolean unsafe;

    public PortableBenchmark(boolean unsafe) {
        this.unsafe = unsafe;
    }

    public void setUp() {

        Config config = new Config();
        config.getSerializationConfig().addPortableFactory(2, new ShoppingCartPortableFactory());

        ClassDefinitionBuilder builder0 = new ClassDefinitionBuilder(2, 1);
        builder0.addIntField("quantity").addLongField("cost").addUTFField("name").
                addBooleanField("stock").addUTFField("url");
        ClassDefinition shoppingCartItemClassDef = builder0.build();

        ClassDefinitionBuilder builder1 = new ClassDefinitionBuilder(2, 2);
        builder1.addLongField("total").addLongField("date").addLongField("id")
        .addPortableArrayField("items", shoppingCartItemClassDef);
        ClassDefinition shoppingCartClassDef = builder1.build();

        config.getSerializationConfig().addClassDefinition(shoppingCartClassDef);
        config.getSerializationConfig().addClassDefinition(shoppingCartItemClassDef);


        hz = Hazelcast.newHazelcastInstance(config);

        cartMap = hz.getMap("carts");
        products = new String[100];
        for (int k = 0; k < 100; k++) {
            products[k] = "product-" + k;
        }
        Random random = new Random();
        SerializationServiceBuilder builder = new DefaultSerializationServiceBuilder();
        builder.setConfig(hz.getConfig().getSerializationConfig());
        SerializationService ss = builder.build();
        int total = 0;
        for (int k = 0; k < maxOrders; k++) {
            ShoppingCart cart = createNewShoppingCart(random);
            Data data = ss.toData(cart);
            total += data.totalSize();
            cartMap.set(cart.id, cart);
        }
        System.out.println("Average size is " + total / maxOrders + " bytes");
    }

    public void tearDown() {
        Hazelcast.shutdownAll();
    }

    public void writePerformance() {
        Random random = new Random();
        for (int k = 0; k < OPERATIONS_PER_INVOCATION; k++) {
            ShoppingCart cart = createNewShoppingCart(random);
            cartMap.set(cart.id, cart);
        }
    }

    public void readPerformance() {
        Random random = new Random();
        for (int k = 0; k < OPERATIONS_PER_INVOCATION; k++) {
            long orderId = random.nextInt(maxOrders);
            cartMap.get(orderId);
        }
    }

    private ShoppingCart createNewShoppingCart(Random random) {
        ShoppingCart cart = new ShoppingCart();
        cart.id = random.nextInt(maxOrders);
        cart.date = new Date();
        int count = random.nextInt(maxCartItems);
        for (int k = 0; k < count; k++) {
            ShoppingCartItem item = createNewShoppingCartItem(random);
            cart.addItem(item);
        }
        return cart;
    }

    private ShoppingCartItem createNewShoppingCartItem(Random random) {
        int i = random.nextInt(10);
        ShoppingCartItem item = new ShoppingCartItem();
        item.cost = i * 9;
        item.quantity = i % 2;
        item.itemName = "item_" + i;
        item.inStock = (i == 9);
        item.url = "http://www.amazon.com/gp/product/" + i;
        return item;
    }
}
