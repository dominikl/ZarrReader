package loci.formats.services;

/*-
 * #%L
 * Implementation of Bio-Formats readers for the next-generation file formats
 * %%
 * Copyright (C) 2020 - 2022 Open Microscopy Environment
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteOrder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bc.zarr.ArrayParams;
import com.bc.zarr.Compressor;
import com.bc.zarr.CompressorFactory;
import com.bc.zarr.DataType;
import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;

import loci.common.services.AbstractService;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.meta.IPyramidStore;
import loci.formats.meta.MetadataRetrieve;
import ucar.ma2.InvalidRangeException;

public class JZarrServiceImpl extends AbstractService
implements ZarrService  {
  // -- Constants --
  private static final Logger LOGGER = LoggerFactory.getLogger(JZarrServiceImpl.class);
  public static final String NO_ZARR_MSG = "JZARR is required to read Zarr files.";

  // -- Fields --
  ZarrArray zarrArray;
  String currentId;
  Compressor zlibComp = CompressorFactory.create("zlib", "level", 8);  // 8 = compression level .. valid values 0 .. 9
  Compressor bloscComp = CompressorFactory.create("blosc", "cname", "lz4hc", "clevel", 7);
  Compressor nullComp = CompressorFactory.create("null");

  /**
   * Default constructor.
   */
  public JZarrServiceImpl(String root) {
      checkClassDependency(com.bc.zarr.ZarrArray.class);
      if (root != null && root.toLowerCase().contains("s3:")) {
        LOGGER.warn("S3 access currently not supported");
      }
  }

  @Override
  public void open(String file) throws IOException, FormatException {
    currentId = file;
    // TODO: Update s3 location identification
    if (!file.toLowerCase().contains("s3:")) {
      zarrArray = ZarrArray.open(file);
    }
    else {
      LOGGER.warn("S3 access currently not supported");
    }
  }
  
  public void open(String id, ZarrArray array) {
    currentId = id;
    zarrArray = array;
  }
  
  public Map<String, Object> getGroupAttr(String path) throws IOException, FormatException {
    ZarrGroup group = null;
    if (!path.toLowerCase().contains("s3:")) {
      group = ZarrGroup.open(path);
    }
    else {
      LOGGER.warn("S3 access currently not supported");
      return null;
    }
    return group.getAttributes();
  }

  public Map<String, Object> getArrayAttr(String path) throws IOException, FormatException {
    ZarrArray array = null;
    if (!path.toLowerCase().contains("s3:")) {
      array = ZarrArray.open(path);
    }
    else {
      LOGGER.warn("S3 access currently not supported");
      return null;
    }
    return array.getAttributes();
  }

  public Set<String> getGroupKeys(String path) throws IOException, FormatException {
    ZarrGroup group = null;
    if (!path.toLowerCase().contains("s3:")) {
      group = ZarrGroup.open(path);
    }
    else {
      LOGGER.warn("S3 access currently not supported");
      return null;
    }
    return group.getGroupKeys();
  }

  public Set<String> getArrayKeys(String path) throws IOException, FormatException {
    ZarrGroup group = null;
    if (!path.toLowerCase().contains("s3:")) {
      group = ZarrGroup.open(path);
    }
    else {
      LOGGER.warn("S3 access currently not supported");
      return null;
    }
    return group.getArrayKeys();
  }

  public DataType getZarrPixelType(int pixType) {
    DataType pixelType = null;
      switch(pixType) {
        case FormatTools.INT8:
          pixelType = DataType.i1;
          break;
        case FormatTools.INT16:
          pixelType = DataType.i2;
          break;
        case FormatTools.INT32:
          pixelType = DataType.i4;
          break;
        case FormatTools.UINT8:
          pixelType = DataType.u1;
          break;
        case FormatTools.UINT16:
          pixelType = DataType.u2;
          break;
        case FormatTools.UINT32:
          pixelType = DataType.u4;
          break;
        case FormatTools.FLOAT:
          pixelType = DataType.f4;
          break;
        case FormatTools.DOUBLE:
          pixelType = DataType.f8;
          break;
      }
      return(pixelType);
  }
  
  public int getOMEPixelType(DataType pixType) {

    int pixelType = -1;
      switch(pixType) {
        case i1:
          pixelType = FormatTools.INT8;
          break;
        case i2:
          pixelType = FormatTools.INT16;
          break;
        case i4:
          pixelType = FormatTools.INT32;
          break;
        case u1:
          pixelType = FormatTools.UINT8;
          break;
        case u2:
          pixelType = FormatTools.UINT16;
          break;
        case u4:
          pixelType = FormatTools.UINT32;
          break;
        case f4:
          pixelType = FormatTools.FLOAT;
          break;
        case f8:
          pixelType = FormatTools.DOUBLE;
          break;
        case i8:
          pixelType = FormatTools.DOUBLE;
          break;
      default:
        break;
      }
      return(pixelType);
  }

  @Override
  public String getNoZarrMsg() {
    return NO_ZARR_MSG;
  }

  @Override
  public int[] getShape() {
    if (zarrArray != null) return zarrArray.getShape();
    return null;
  }

  @Override
  public int[] getChunkSize() {
    if (zarrArray != null) return zarrArray.getChunks();
    return null;
  }

  @Override
  public int getPixelType() {
    if (zarrArray != null) return getOMEPixelType(zarrArray.getDataType());
    return 0;
  }

  @Override
  public boolean isLittleEndian() {
    if (zarrArray != null) return (zarrArray.getByteOrder().equals(ByteOrder.LITTLE_ENDIAN));
    return false;
  }

  @Override
  public void close() throws IOException {
    zarrArray = null;
    currentId = null;
  }

  @Override
  public boolean isOpen() {
    return (zarrArray != null && currentId != null);
  }

  @Override
  public String getID() {
    return currentId;
  }

  @Override
  public Object readBytes(int[] shape, int[] offset) throws FormatException, IOException {
    if (zarrArray != null) {
      try {
        return zarrArray.read(shape, offset);
      } catch (InvalidRangeException e) {
        throw new FormatException(e);
      }
    }
    else throw new IOException("No Zarr file opened");
  }

  @Override
  public void saveBytes(Object data, int[] shape, int[] offset) throws FormatException, IOException {
    if (zarrArray != null) {
      try {
        zarrArray.write(data, shape, offset);
      } catch (InvalidRangeException e) {
        throw new FormatException(e);
      }
    }
    else throw new IOException("No Zarr file opened");
  }

  @Override
  public void create(String file, MetadataRetrieve meta, int[] chunks, Compression compression) throws IOException {
    int seriesCount = meta.getImageCount();
    int resolutionCount = 1;

    ArrayParams params = new ArrayParams();
    params.chunks(chunks);
    params.compressor(nullComp);

    boolean isLittleEndian = !meta.getPixelsBigEndian(0);
    if (isLittleEndian) {
      params.byteOrder(ByteOrder.LITTLE_ENDIAN);
    }

    int x = meta.getPixelsSizeX(0).getValue().intValue();
    int y = meta.getPixelsSizeY(0).getValue().intValue();
    int z = meta.getPixelsSizeZ(0).getValue().intValue();
    int c = meta.getPixelsSizeC(0).getValue().intValue();
    int t = meta.getPixelsSizeT(0).getValue().intValue();
   // c /= meta.getChannelSamplesPerPixel(0, 0).getValue().intValue();
    int [] shape = {x, y, z, c, t};
    params.shape(shape);

    int pixelType = FormatTools.pixelTypeFromString(meta.getPixelsType(0).toString());
    DataType zarrPixelType = getZarrPixelType(pixelType);
    int bytes = FormatTools.getBytesPerPixel(pixelType);
    params.dataType(zarrPixelType);

    if (seriesCount > 1) {
      ZarrGroup root = ZarrGroup.create(file);
      ZarrGroup currentGroup = root;
      for (int i = 0; i < seriesCount; i++) {
        x = meta.getPixelsSizeX(i).getValue().intValue();
        y = meta.getPixelsSizeY(i).getValue().intValue();
        z = meta.getPixelsSizeZ(i).getValue().intValue();
        c = meta.getPixelsSizeC(i).getValue().intValue();
        t = meta.getPixelsSizeT(i).getValue().intValue();
      //  c /= meta.getChannelSamplesPerPixel(i, 0).getValue().intValue();
        shape = new int[]{x, y, z, c, t};
        params.shape(shape);

        pixelType = FormatTools.pixelTypeFromString(meta.getPixelsType(i).toString());
        zarrPixelType = getZarrPixelType(pixelType);
        params.dataType(zarrPixelType);

        isLittleEndian = !meta.getPixelsBigEndian(i);
        if (isLittleEndian) {
          params.byteOrder(ByteOrder.LITTLE_ENDIAN);
        }

        if (meta instanceof IPyramidStore) {
          resolutionCount = ((IPyramidStore) meta).getResolutionCount(i);
        }
        if (resolutionCount > 1) {
          currentGroup = root.createSubGroup("Series"+i);
          for (int j = 0; j < resolutionCount; j++) {
            zarrArray = currentGroup.createArray("Resolution"+j, params);
          }
        }
        else {
          zarrArray = currentGroup.createArray("Series"+i, params);
        }
      }
    }
    else {
      zarrArray = ZarrArray.create(file, params);
    }
    currentId = file;
  }

  @Override
  public void create(String id, MetadataRetrieve meta, int[] chunks) throws IOException {
    create(id, meta, chunks, Compression.NONE);
  }


}
