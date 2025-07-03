package com.antz.jolt.util;

import jolt.core.TempAllocatorImpl;
import jolt.Jolt;
import jolt.core.Factory;
import jolt.core.JobSystemThreadPool;
import jolt.physics.PhysicsSystem;
import jolt.physics.collision.ObjectLayerPairFilterTable;
import jolt.physics.collision.broadphase.BroadPhaseLayer;
import jolt.physics.collision.broadphase.BroadPhaseLayerInterfaceTable;
import jolt.physics.collision.broadphase.ObjectVsBroadPhaseLayerFilterTable;

public class JoltInstance {

    private PhysicsSystem physicsSystem;
    private Factory factory;
    private ObjectVsBroadPhaseLayerFilterTable mObjectVsBroadPhaseLayerFilter;
    private BroadPhaseLayer BP_LAYER_NON_MOVING;
    private BroadPhaseLayer BP_LAYER_MOVING;
    private BroadPhaseLayerInterfaceTable mBroadPhaseLayerInterface;
    private ObjectLayerPairFilterTable mObjectLayerPairFilter;
    private TempAllocatorImpl mTempAllocator;
    private JobSystemThreadPool mJobSystem;

    public JoltInstance() {
        Jolt.Init();

        int mMaxBodies = 10240;
        int mMaxBodyPairs = 65536;
        int mMaxContactConstraints = 20480;
        int mTempAllocatorSize = 32 * 1024 * 1024; // 32 MB
        int cNumBodyMutexes = 0;

        // Layer that objects can be in, determines which other objects it can collide with
        // Typically you at least want to have 1 layer for moving bodies and 1 layer for static bodies, but you can have more
        // layers if you want. E.g. you could have a layer for high detail collision (which is not used by the physics simulation
        // but only if you do collision testing).

        mObjectLayerPairFilter = new ObjectLayerPairFilterTable(JoltLayers.NUM_LAYERS);
        mObjectLayerPairFilter.EnableCollision(JoltLayers.NON_MOVING, JoltLayers.MOVING);
        mObjectLayerPairFilter.EnableCollision(JoltLayers.MOVING, JoltLayers.MOVING);

        // Each broadphase layer results in a separate bounding volume tree in the broad phase. You at least want to have
        // a layer for non-moving and moving objects to avoid having to update a tree full of static objects every frame.
        // You can have a 1-on-1 mapping between object layers and broadphase layers (like in this case) but if you have
        // many object layers you'll be creating many broad phase trees, which is not efficient.

        int NUM_BROAD_PHASE_LAYERS = 2;
        mBroadPhaseLayerInterface = new BroadPhaseLayerInterfaceTable(JoltLayers.NUM_LAYERS, NUM_BROAD_PHASE_LAYERS);
        BP_LAYER_NON_MOVING = new BroadPhaseLayer((short)0);
        mBroadPhaseLayerInterface.MapObjectToBroadPhaseLayer(JoltLayers.NON_MOVING, BP_LAYER_NON_MOVING);
        BP_LAYER_MOVING = new BroadPhaseLayer((short)1);
        mBroadPhaseLayerInterface.MapObjectToBroadPhaseLayer(JoltLayers.MOVING, BP_LAYER_MOVING);

        mObjectVsBroadPhaseLayerFilter = new ObjectVsBroadPhaseLayerFilterTable(mBroadPhaseLayerInterface, NUM_BROAD_PHASE_LAYERS, mObjectLayerPairFilter, JoltLayers.NUM_LAYERS);

        mTempAllocator = Jolt.New_TempAllocatorImpl(mTempAllocatorSize);
        mJobSystem = Jolt.New_JobSystemThreadPool(11); // 12 threads on Ryzen 5 - 1 = 11

        factory = Jolt.New_Factory();
        Factory.set_sInstance(factory);
        Jolt.RegisterTypes();
        physicsSystem = Jolt.New_PhysicsSystem();
        physicsSystem.Init(mMaxBodies, cNumBodyMutexes, mMaxBodyPairs, mMaxContactConstraints, mBroadPhaseLayerInterface, mObjectVsBroadPhaseLayerFilter, mObjectLayerPairFilter);
    }

    public PhysicsSystem getPhysicsSystem() {
        return physicsSystem;
    }

    public JobSystemThreadPool getJobSystem() {
        return mJobSystem;
    }

    public void update(float deltaTime, int inCollisionSteps) {
        physicsSystem.Update(deltaTime, inCollisionSteps, mTempAllocator, mJobSystem);
    }

    public void clearWorld() {
        Jolt.ClearWorld(physicsSystem);
    }

    public void dispose() {
        physicsSystem.dispose();
        BP_LAYER_NON_MOVING.dispose();
        BP_LAYER_MOVING.dispose();
        mObjectLayerPairFilter.dispose();

        Factory.set_sInstance(null);
        factory.dispose();
        Jolt.UnregisterTypes();
    }
}
