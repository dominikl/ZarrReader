package loci.formats.services;

import java.io.File;

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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.zarr.zarrjava.ZarrException;
import dev.zarr.zarrjava.core.Array;
import dev.zarr.zarrjava.core.DataType;
import loci.common.services.AbstractService;
import loci.formats.FormatException;
import loci.formats.FormatTools;

public class JZarrServiceImpl extends AbstractService implements ZarrService  {

  private static final Logger LOGGER = LoggerFactory.getLogger(JZarrServiceImpl.class);

  public static final String NO_ZARR_MSG = "zarr-java is required to read Zarr files.";

  ZarrLocation zarr;

  Array array;

  Map<String, Object> attr;


  /**
   * Default constructor.
   */
  public JZarrServiceImpl(ZarrLocation zarr) {
      checkClassDependency(Array.class);
      this.zarr = zarr;
  }

  @Override
  public void open(String path) throws IOException, FormatException {
      array = zarr.getArray(path);
      attr = zarr.metadataFromArray(path);
  }

  private boolean isV2() {
    return array instanceof dev.zarr.zarrjava.v2.Array;
  }

  public DataType getZarrPixelType(int pixType) {
    DataType pixelType = null;
      switch(pixType) {
        case FormatTools.INT8:
          pixelType = isV2() ? dev.zarr.zarrjava.v2.DataType.INT8 :  dev.zarr.zarrjava.v3.DataType.INT8;
          break;
        case FormatTools.INT16:
          pixelType = isV2() ? dev.zarr.zarrjava.v2.DataType.INT16 : dev.zarr.zarrjava.v3.DataType.INT16;
          break;
        case FormatTools.INT32:
          pixelType = isV2() ? dev.zarr.zarrjava.v2.DataType.INT32 : dev.zarr.zarrjava.v3.DataType.INT32;
          break;
        case FormatTools.UINT8:
          pixelType = isV2() ? dev.zarr.zarrjava.v2.DataType.UINT8 : dev.zarr.zarrjava.v3.DataType.UINT8;
          break;
        case FormatTools.UINT16:
          pixelType = isV2() ? dev.zarr.zarrjava.v2.DataType.UINT16 : dev.zarr.zarrjava.v3.DataType.UINT16;
          break;
        case FormatTools.UINT32:
          pixelType = isV2() ? dev.zarr.zarrjava.v2.DataType.UINT32 : dev.zarr.zarrjava.v3.DataType.UINT32;
          break;
        case FormatTools.FLOAT:
          pixelType = isV2() ? dev.zarr.zarrjava.v2.DataType.FLOAT32 : dev.zarr.zarrjava.v3.DataType.FLOAT32;
          break;
        case FormatTools.DOUBLE:
          pixelType = isV2() ? dev.zarr.zarrjava.v2.DataType.FLOAT64 : dev.zarr.zarrjava.v3.DataType.FLOAT64;
          break;
      }
      return(pixelType);
  }
  
  public int getOMEPixelType(DataType pixType) {
    int pixelType = -1;
    if (isV2()) {
      dev.zarr.zarrjava.v2.DataType dt = (dev.zarr.zarrjava.v2.DataType)pixType;
      switch(dt) {
        case INT8:
          pixelType = FormatTools.INT8;
          break;
        case INT16:
          pixelType = FormatTools.INT16;
          break;
        case INT32:
          pixelType = FormatTools.INT32;
          break;
        case UINT8:
          pixelType = FormatTools.UINT8;
          break;
        case UINT16:
          pixelType = FormatTools.UINT16;
          break;
        case UINT32:
          pixelType = FormatTools.UINT32;
          break;
        case FLOAT32:
          pixelType = FormatTools.FLOAT;
          break;
        case FLOAT64:
          pixelType = FormatTools.DOUBLE;
          break;
      default:
        break;
      }
    }
    else {
      dev.zarr.zarrjava.v3.DataType dt = (dev.zarr.zarrjava.v3.DataType)pixType;
      switch(dt) {
        case INT8:
          pixelType = FormatTools.INT8;
          break;
        case INT16:
          pixelType = FormatTools.INT16;
          break;
        case INT32:
          pixelType = FormatTools.INT32;
          break;
        case UINT8:
          pixelType = FormatTools.UINT8;
          break;
        case UINT16:
          pixelType = FormatTools.UINT16;
          break;
        case UINT32:
          pixelType = FormatTools.UINT32;
          break;
        case FLOAT32:
          pixelType = FormatTools.FLOAT;
          break;
        case FLOAT64:
          pixelType = FormatTools.DOUBLE;
          break;
      default:
        break;
      }
    }
      return(pixelType);
  }

  @Override
  public String getNoZarrMsg() {
    return NO_ZARR_MSG;
  }

  @Override
  public int[] getShape() {
    if (attr != null && attr.containsKey("shape")) {
      long[] shape = (long[]) attr.get("shape");
      int[] res = new int[shape.length];
      for (int i = 0; i < shape.length; i++) {
        res[i] = (int) shape[i];
      }
      return res;
    }
    return null;
  }

  @Override
  public int[] getChunkSize() {
    if (attr != null && attr.containsKey("chunkShape")) 
      return (int[]) attr.get("chunkShape");
    return null;
  }

  @Override
  public int getPixelType() {
    if (attr != null && attr.containsKey("dataType")) 
      return getOMEPixelType((DataType) attr.get("dataType"));
    return 0;
  }

  @Override
  public boolean isLittleEndian() {
    if (attr != null && attr.containsKey("littleEndian")) 
      return (boolean) attr.get("littleEndian");
    return false;
  }

  @Override
  public void close() throws IOException {
    array = null;
    attr = null;
  }

  @Override
  public boolean isOpen() {
    return (array != null);
  }
  
  @Override
  public Object readBytes(int[] shape, int[] offset) throws FormatException, IOException {
    if (array != null) {
      try {
        long[] offsetLong = new long[offset.length];
        for (int i = 0; i < offset.length; i++) {
          offsetLong[i] = offset[i];
        }
        return array.read(offsetLong, shape).getDataAsByteBuffer().array();
      } catch (ZarrException e) {
        throw new FormatException(e);
      }
    }
    else throw new IOException("No Zarr file opened");
  }
}
