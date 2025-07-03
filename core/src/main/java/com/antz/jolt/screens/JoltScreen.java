package com.antz.jolt.screens;

import com.antz.jolt.util.AntzFPSLogger;
import com.antz.jolt.util.JoltInstance;
import com.antz.jolt.util.JoltLayers;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import jolt.Jolt;
import jolt.enums.EActivation;
import jolt.enums.EMotionType;
import jolt.gdx.DebugRenderer;
import jolt.gdx.JoltGdx;
import jolt.math.Mat44;
import jolt.math.Quat;
import jolt.math.Vec3;
import jolt.physics.PhysicsSystem;
import jolt.physics.body.*;
import jolt.physics.collision.shape.BoxShape;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;


public class JoltScreen implements Screen {

    public static final float TARGET_FRAME_LENGTH = 1f/60f;
    public static final float DEATH_FRAME_LENGTH = 1f/30f;

    private JoltInstance joltInstance;
    protected PhysicsSystem mPhysicsSystem = null;
    protected BodyInterface mBodyInterface = null;

    protected PerspectiveCamera camera;
    private ScreenViewport viewport;

    private AntzFPSLogger fpsLogger = new AntzFPSLogger();
    private float resetDelaySeconds = 14;
    private float timer, labelTimer, physicsTimer;

    private int totalCubes = 3000;
    private int cubeCount = 0;
    private int iteration = 0;
    private Array<CubeData> cubes = new Array<>();
    private CubeData groundData;

    private Vec3 tempVec3;
    private Quat tempQuat;
    private Quaternion tempQuaternion;
    private Matrix4 tempRotationMatrix;
    private Model cubeModel;
    private Model cubeModelInstanced;
    private Matrix4 instanceTransform = new Matrix4();

    private SceneManager sceneManager;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;

    private Texture checkerBoardTexture;
    private Texture boxTexture;
    private float boxRestitution = 0.95f;

    private SpriteBatch batch2D = new SpriteBatch();
    private BitmapFont font = new BitmapFont();

    private boolean renderModels = true;  // false = make it a physics only test

    @Override
    public void show() {
        fpsLogger.onlyReportMode = true; // just output final iteration report
        joltInstance = new JoltInstance();
        setPhysicsSystem(joltInstance.getPhysicsSystem());

        camera = new PerspectiveCamera();
        viewport = new ScreenViewport(camera);
        camera.far = 1000f;
        camera.up.set(0, 1, 0);
        camera.position.set(30, 10, 30);
        camera.lookAt(0, 0, 0);

        tempVec3 = Jolt.New_Vec3();
        tempQuat = new Quat();
        tempQuaternion = new Quaternion();
        tempRotationMatrix = new Matrix4();

        sceneManager = new SceneManager();
        DirectionalLightEx light = new DirectionalLightEx();
        light.direction.set(-0.9f, -1, -1);
        light.direction.nor();
        light.color.set(Color.WHITE);
        light.intensity = 8.8f;
        sceneManager.environment.add(light);

        sceneManager.setAmbientLight(0.5f);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        createModels();
        resetBoxes();
        updateModels();
    }

    @Override
    public void render(float delta) {
        checkInput();
        fpsLogger.log();

        ScreenUtils.clear(Color.RED, true);
        camera.update();
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        physicsTimer += delta;
        timer += delta;

        if (timer > resetDelaySeconds){
            timer = 0;
            fpsLogger.reset("libGDX iteration " + iteration + " final report >>>");
            iteration++;
            if (iteration > 5){
                Gdx.app.log("JoltTest", "*** Test has finished! ***");
                Gdx.app.exit();
            }
            resetBoxes();
        }

        // Since libGDX is an incomplete framework for 3D,
        // it does not have any sense of physics ticks or a
        // physics process like render(float delta) so I have
        // to create my own.  Targets 60 physics ticks per second.
        if (physicsTimer >= TARGET_FRAME_LENGTH) {
            stepPhysics(TARGET_FRAME_LENGTH);
            physicsTimer -= TARGET_FRAME_LENGTH;

            if (physicsTimer > TARGET_FRAME_LENGTH) physicsTimer = 0;
        }

        renderModels(delta);
        renderLabels(delta);
    }

    public void stepPhysics(float deltaTime) {
        // When running below 30 Hz, do 2 steps instead of 1
        var numSteps = deltaTime > DEATH_FRAME_LENGTH ? 2 : 1;
        joltInstance.update(deltaTime, numSteps);
        updateModels();
    }

    private void updateModels() {
        if (renderModels) {
            for (int i = 0; i < cubes.size; i++) {
                CubeData cubeData = cubes.get(i);
                Body body = cubeData.body;
                Mat44 mat44 = body.GetWorldTransform();
                instanceTransform.idt();
                JoltGdx.mat44_to_matrix4(mat44, instanceTransform);
                cubeData.modelInstance.transform.set(instanceTransform);
            }
        }
    }

    private void renderLabels(float delta) {
        labelTimer += delta;
        if (labelTimer > 1.5) {
            batch2D.begin();
            font.draw(batch2D, "Iteration: " + iteration, 2, 60);
            font.draw(batch2D, "Box count: " + cubeCount, 2, 45);
            font.draw(batch2D, "FPS: " + fpsLogger.fps, 2, 30);
            font.draw(batch2D, "MIN: " + fpsLogger.min + "   AVG: " + fpsLogger.average + "   MAX: " + fpsLogger.max, 2, 15);
            batch2D.end();
        }
    }

    private void renderModels(float delta) {
        JoltGdx.mat44_to_matrix4(groundData.body.GetWorldTransform(), groundData.modelInstance.transform);

        sceneManager.getRenderableProviders().clear();

        if (renderModels) {
            for (int i = 0; i < cubes.size; i++) {
                sceneManager.getRenderableProviders().add(cubes.get(i).modelInstance);
            }
        }

        sceneManager.getRenderableProviders().add(groundData.modelInstance);
        sceneManager.setCamera(camera);
        sceneManager.update(delta);
        sceneManager.render();
    }

    private void checkInput(){
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            Gdx.app.exit();
    }

    private void createModels() {
        boxTexture = new Texture(Gdx.files.internal("textures/badlogic.jpg"));
        boxTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        final Material material = new Material(PBRTextureAttribute.createBaseColorTexture(boxTexture),
            FloatAttribute.createShininess(4f));
        ModelBuilder builder = new ModelBuilder();
        cubeModel = builder.createBox(1, 1, 1, material, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        cubeModelInstanced = builder.createBox(1, 1, 1, material, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);

        float groundWidth = 60f;
        float groundHeight = 0.3f;
        float groundDepth = 60f;
        checkerBoardTexture = DebugRenderer.createCheckerBoardTexture();
        final Material groundMaterial = new Material(
            PBRTextureAttribute.createBaseColorTexture(checkerBoardTexture),
            ColorAttribute.createDiffuse(1, 1, 1, 1)
        );
        int attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorUnpacked;
        Model groundBox = builder.createBox(groundWidth, groundHeight, groundDepth, groundMaterial, attributes);
        groundData = createBox(new ModelInstance(groundBox), -2, -1, 0, -2, 0, 0, 0, 0, groundWidth, groundHeight, groundDepth);
    }

    private CubeData createBox(ModelInstance modelInstance, int userData, float mass, float x, float y, float z, float axiX, float axiY, float axiZ, float x1, float y1, float z1) {
        tempVec3.Set(x1 / 2f, y1 / 2f, z1 / 2f);
        BoxShape bodyShape = new BoxShape(tempVec3);

        EMotionType motionType = EMotionType.Dynamic;
        int layer = JoltLayers.MOVING;

        tempRotationMatrix.idt();
        tempRotationMatrix.rotate(Vector3.X, axiX);
        tempRotationMatrix.rotate(Vector3.Y, axiY);
        tempRotationMatrix.rotate(Vector3.Z, axiZ);
        tempRotationMatrix.getRotation(tempQuaternion);

        tempVec3.Set(x, y, z);
        tempQuat.Set(tempQuaternion.x, tempQuaternion.y, tempQuaternion.z, tempQuaternion.w);

        MassProperties massProperties = bodyShape.GetMassProperties();
        if(mass > 0.0f) {
            massProperties.set_mMass(mass);
        }
        else if(mass < 0.0f) {
            motionType = EMotionType.Static;
            layer = JoltLayers.NON_MOVING;
        }

        BodyCreationSettings bodySettings = Jolt.New_BodyCreationSettings(bodyShape, tempVec3, tempQuat, motionType, layer);
        bodySettings.set_mMassPropertiesOverride(massProperties);
        bodySettings.set_mRestitution(boxRestitution);
        bodySettings.set_mFriction(0.2f);
        Body body = mBodyInterface.CreateBody(bodySettings);
        body.SetUserData(userData);
        bodySettings.dispose();

        CubeData cubeData = new CubeData();
        cubeData.body = body;
        cubeData.modelInstance = modelInstance;
        mBodyInterface.AddBody(body.GetID(), EActivation.Activate);
        return cubeData;
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    private void resetBoxes() {
        labelTimer = 0;
        BodyInterface bodyInterface = mPhysicsSystem.GetBodyInterface();
        for(CubeData cubeData : cubes) {
            Body body = cubeData.body;
            BodyID bodyID = body.GetID();
            bodyInterface.RemoveBody(bodyID);
            bodyInterface.DestroyBody(bodyID);
        }
        cubes.clear();

        int base = (int) Math.round(Math.cbrt(totalCubes));
        int maxX = base;
        int maxY = base;
        int maxZ = base;

        float multi = 1.3f;
        int offsetY = 22;
        int offsetX = -8;
        int offsetZ = -6;
        cubeCount = 0;
        for(int i = 0; i < maxX; i++) {
            for(int j = 0; j < maxY; j++) {
                for(int k = 0; k < maxZ; k++) {
                    if(cubeCount < totalCubes) {
                        float x = (i + offsetX) * multi;
                        float y = (j + offsetY) * multi;
                        float z = (k + offsetZ) * multi;
                        float axisX = 1;
                        float axisY = 1;
                        float axisZ = 1;

                        ModelInstance instance = new ModelInstance(cubeModel);

                        CubeData box = createBox(instance, cubeCount, 0.4f, x, y, z, axisX, axisY, axisZ, 1, 1, 1);
                        cubes.add(box);
                        cubeCount++;

                        if(i == maxX-1 && j == maxY-1 && k == maxZ-1) {
                            if(cubeCount != totalCubes) {
                                maxX++;
                            }
                        }
                    }
                    else {
                        mPhysicsSystem.OptimizeBroadPhase();
                        return;
                    }
                }
            }
        }
        mPhysicsSystem.OptimizeBroadPhase();
    }

    public void setPhysicsSystem(PhysicsSystem mPhysicsSystem) {
        this.mPhysicsSystem = mPhysicsSystem;
        mBodyInterface = mPhysicsSystem.GetBodyInterface();
    }

    @Override
    public void dispose() {
        checkerBoardTexture.dispose();
        boxTexture.dispose();
        cubeModel.dispose();
        cubeModelInstanced.dispose();
        tempVec3.dispose();
        tempQuat.dispose();

        sceneManager.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
    }

    static class CubeData {
        public Body body;
        public ModelInstance modelInstance;
    }
}
