package client.model;

import com.cleanroommc.pointer.BlockPointer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Tuple4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.minecraft.util.EnumFacing.DOWN;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.UP;
import static net.minecraft.util.EnumFacing.WEST;

public class BlockBakedModel implements IBakedModel {
    private final Matrix4f tempModelMatrix = new Matrix4f();
    private final Vector3f PIVOT = new Vector3f(.5F, .5F, .5F);
    private final Vector3f APIVOT = new Vector3f(-.5F, -.5F, -.5F);

    public BlockBakedModel() {
        super();
    }

    void moveToPivot(Matrix4f matrix, Vector3f pivot) {
        this.tempModelMatrix.setIdentity();
        this.tempModelMatrix.setTranslation(pivot);
        matrix.mul(this.tempModelMatrix);
    }

    void rotateX(Matrix4f matrix, float angle) {
        this.tempModelMatrix.setIdentity();
        this.tempModelMatrix.rotX(angle);
        matrix.mul(this.tempModelMatrix);
    }

    void rotateY(Matrix4f matrix, float angle) {
        this.tempModelMatrix.setIdentity();
        this.tempModelMatrix.rotY(angle);
        matrix.mul(this.tempModelMatrix);
    }

    void rotateZ(Matrix4f matrix, float angle) {
        this.tempModelMatrix.setIdentity();
        this.tempModelMatrix.rotZ(angle);
        matrix.mul(this.tempModelMatrix);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (side != null) {
            return Collections.emptyList();
        }

        IExtendedBlockState extState = (IExtendedBlockState) state;

        EnumFacing[] f = extState.getValue(BlockPointer.FACING);
        ModelResourceLocation mrl = new ModelResourceLocation(state.getBlock().getRegistryName(), "normal");
        IBakedModel m = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getModel(mrl);

        List<BakedQuad> q = m.getQuads(extState, side, rand);

        Matrix4f transformMatrix = new Matrix4f();
        this.tempModelMatrix.setIdentity();

        final Tuple4f vertexTransformingVec = new Vector4f();
        if (f == null) return Collections.emptyList();

        EnumFacing topFacing = f[0];
        EnumFacing frontFacing = f[1];

        List<BakedQuad> result = new ArrayList<>();
        for (BakedQuad b : q) {
            transformMatrix.setIdentity();
            moveToPivot(transformMatrix, PIVOT);

            if (topFacing == UP || topFacing == DOWN) {
                if (frontFacing == NORTH) {
                    rotateY(transformMatrix, (float) (Math.PI));
                } else if (frontFacing == EAST) {
                    rotateY(transformMatrix, (float) (Math.PI / 2));
                } else if (frontFacing == WEST) {
                    rotateY(transformMatrix, (float) (-Math.PI / 2));
                } else if (frontFacing == SOUTH) {
                    //rotateY(transformMatrix, (float) (Math.PI));
                }
                if (topFacing == DOWN) {
                    rotateX(transformMatrix, (float) (Math.PI));
                    rotateY(transformMatrix, (float) (Math.PI));
                }
            }
            else {
                if (topFacing == WEST) {
                    rotateZ(transformMatrix, (float) (Math.PI / 2));
                    if (frontFacing == DOWN) {
                        rotateY(transformMatrix, (float) (-Math.PI/ 2));
                    } else if (frontFacing == UP) {
                        rotateY(transformMatrix, (float) (+Math.PI/ 2));
                    }
                } else if (topFacing == EAST) {
                    rotateZ(transformMatrix, (float) (-Math.PI / 2));
                    if (frontFacing == DOWN) {
                        rotateY(transformMatrix, (float) (Math.PI/ 2));
                    } else if (frontFacing == UP) {
                        rotateY(transformMatrix, (float) (-Math.PI/ 2));
                    }
                } else if (topFacing == NORTH) {
                    rotateX(transformMatrix, (float) (-Math.PI / 2));
                    if (frontFacing == DOWN) {
                        rotateY(transformMatrix, (float) (Math.PI));
                    }
                } else {
                    rotateX(transformMatrix, (float) (Math.PI / 2));
                    if (frontFacing == UP) {
                        rotateY(transformMatrix, (float) (Math.PI));
                    }
                }
            }

            moveToPivot(transformMatrix, APIVOT);

            int[] newQuad = new int[28];
            int[] quadData = b.getVertexData();
            for (int k = 0; k < 4; ++k) {
                // Getting the offset for the current vertex.
                int vertexIndex = k * 7;
                vertexTransformingVec.x = Float.intBitsToFloat(quadData[vertexIndex]);
                vertexTransformingVec.y = Float.intBitsToFloat(quadData[vertexIndex + 1]);
                vertexTransformingVec.z = Float.intBitsToFloat(quadData[vertexIndex + 2]);
                vertexTransformingVec.w = 1;

                // Transforming it by the model matrix.
                transformMatrix.transform(vertexTransformingVec);

                // Converting the new data to ints.
                int x = Float.floatToRawIntBits((float) (vertexTransformingVec.x));
                int y = Float.floatToRawIntBits((float) (vertexTransformingVec.y));
                int z = Float.floatToRawIntBits((float) (vertexTransformingVec.z));

                // vertex position data
                newQuad[vertexIndex] = x;
                newQuad[vertexIndex + 1] = y;
                newQuad[vertexIndex + 2] = z;

                newQuad[vertexIndex + 3] = quadData[vertexIndex + 3];

                newQuad[vertexIndex + 4] = quadData[vertexIndex + 4]; //texture
                newQuad[vertexIndex + 5] = quadData[vertexIndex + 5];

                // vertex brightness
                newQuad[vertexIndex + 6] = quadData[vertexIndex + 6];
            }
            b = new BakedQuad(newQuad, b.getTintIndex(), b.getFace(), b.getSprite(), true, DefaultVertexFormats.BLOCK);
            result.add(b);
        }
        return result;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return null;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return null;
    }
}
