package com.cy.plugin.id;

import com.cy.plugin.id.impl.SnowflakeUpperGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * @classDesc: 功能描述:(主键id生成器)
 * @author: cyjer
 * @date: 2022/11/21 15:30
 */
public class IdGenerator {

    private static final Logger log = LoggerFactory.getLogger(IdGenerator.class);

    public static final class IdGeneratorHolder {
        public static final IdGenerator GENERATOR = new IdGenerator();
    }

    public static IdGenerator ins() {
        return IdGeneratorHolder.GENERATOR;
    }

    private IdGeneratorInterface defaultGenerator;

    public IdGenerator() {
        Random random = new Random();
        long randomServiceId = random.nextInt(256);
        this.defaultGenerator = new SnowflakeUpperGenerator(randomServiceId);
    }

    /**
     * 生成唯一ID
     *
     * @return
     */
    public Long generator() {
        Long generator = defaultGenerator.generator();
        return generator;
    }


    public static void main(String[] args) {
        Long id = IdGenerator.ins().generator();
        System.out.println(id);
    }
}
