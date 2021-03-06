/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.datagrid.carmart.session;

import org.infinispan.commons.api.BasicCache;
import org.jboss.as.quickstarts.datagrid.carmart.model.Car;

import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Adds, retrieves, removes new cars from the cache. Also returns a list of cars stored in the cache.
 * 
 * @author Martin Gencur
 * 
 */
@Model
public class CarManager {

    private Logger log = Logger.getLogger(this.getClass().getName());

    public static final String CACHE_NAME = "carcache";

    public static final String CAR_NUMBERS_KEY = "carnumbers";

    @Inject
    private CacheContainerProvider provider;

    /*
     * Injects the javax.transaction.UserTransaction - The TransactionManager lookup is configured on
     * JBossASCacheContainerProvider/TomcatCacheContainerProvider impl classes for CacheContainerProvider
     */
    @Inject
    private UserTransaction utx;

    private BasicCache<String, Object> carCache;

    private String carId;
    private Car car = new Car();

    public CarManager() {
    }

    public String addNewCar() {
        carCache = provider.getCacheContainer().getCache(CACHE_NAME);
        try {
            utx.begin();
            List<String> carNumbers = getNumberPlateList(carCache);
            carNumbers.add(car.getNumberPlate());
            carCache.put(CAR_NUMBERS_KEY, carNumbers);
            carCache.put(CarManager.encode(car.getNumberPlate()), car);
            utx.commit();
        } catch (Exception e) {
            if (utx != null) {
                try {
                    utx.rollback();
                } catch (Exception e1) {
                }
            }
        }
        return "home";
    }

    public String addNewCarWithRollback() {
        boolean throwInducedException = true;
        carCache = provider.getCacheContainer().getCache(CACHE_NAME);
        try {
            utx.begin();
            List<String> carNumbers = getNumberPlateList(carCache);
            carNumbers.add(car.getNumberPlate());
            // store the new list of car numbers and then throw an exception -> roll-back
            // the car number list should not be stored in the cache
            carCache.put(CAR_NUMBERS_KEY, carNumbers);
            if (throwInducedException)
                throw new RuntimeException("Induced exception");
            carCache.put(CarManager.encode(car.getNumberPlate()), car);
            utx.commit();
        } catch (Exception e) {
            if (utx != null) {
                try {
                    utx.rollback();
                    log.info("Rolled back due to: " + e.getMessage());
                } catch (Exception e1) {
                }
            }
        }
        return "home";
    }

    /**
     * Operate on a clone of car number list so that we can demonstrate transaction roll-back.
     */
    @SuppressWarnings("unchecked")
    private List<String> getNumberPlateList(BasicCache<String, Object> carCacheLoc) {
        List<String> result = null;
        List<String> carNumberList = (List<String>) carCacheLoc.get(CAR_NUMBERS_KEY);
        if (carNumberList == null) {
            result = new LinkedList<>();
        } else {
            result = new LinkedList<>(carNumberList);
        }
        return result;
    }

    public String showCarDetails(String numberPlate) {
        carCache = provider.getCacheContainer().getCache(CACHE_NAME);
        try {
            utx.begin();
            this.car = (Car) carCache.get(encode(numberPlate));
            utx.commit();
        } catch (Exception e) {
            if (utx != null) {
                try {
                    utx.rollback();
                } catch (Exception e1) {
                }
            }
        }
        return "showdetails";
    }

    public List<String> getCarList() {
        List<String> result = null;
        try {
            utx.begin();
            // retrieve a cache
            carCache = provider.getCacheContainer().getCache(CACHE_NAME);
            // retrieve a list of number plates from the cache
            result = getNumberPlateList(carCache);
            utx.commit();
        } catch (Exception e) {
            if (utx != null) {
                try {
                    utx.rollback();
                } catch (Exception e1) {
                }
            }
        }
        return result;
    }

    public String removeCar(String numberPlate) {
        carCache = provider.getCacheContainer().getCache(CACHE_NAME);
        try {
            utx.begin();
            carCache.remove(encode(numberPlate));
            List<String> carNumbers = getNumberPlateList(carCache);
            carNumbers.remove(numberPlate);
            carCache.put(CAR_NUMBERS_KEY, carNumbers);
            utx.commit();
        } catch (Exception e) {
            if (utx != null) {
                try {
                    utx.rollback();
                } catch (Exception e1) {
                }
            }
        }
        return null;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getCarId() {
        return carId;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Car getCar() {
        return car;
    }

    public static String encode(String key) {
        try {
            return URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decode(String key) {
        try {
            return URLDecoder.decode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
