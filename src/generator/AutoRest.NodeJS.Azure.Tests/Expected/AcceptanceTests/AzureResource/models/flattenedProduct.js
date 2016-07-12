/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 * 
 * Code generated by Microsoft (R) AutoRest Code Generator.
 * Changes may cause incorrect behavior and will be lost if the code is
 * regenerated.
 */

'use strict';

var models = require('./index');

var util = require('util');

/**
 * @class
 * Initializes a new instance of the FlattenedProduct class.
 * @constructor
 * @member {string} [pname]
 * 
 * @member {number} [lsize]
 * 
 * @member {string} [provisioningState]
 * 
 */
function FlattenedProduct() {
  FlattenedProduct['super_'].call(this);
}

util.inherits(FlattenedProduct, models['Resource']);

/**
 * Defines the metadata of FlattenedProduct
 *
 * @returns {object} metadata of FlattenedProduct
 *
 */
FlattenedProduct.prototype.mapper = function () {
  return {
    required: false,
    serializedName: 'FlattenedProduct',
    type: {
      name: 'Composite',
      className: 'FlattenedProduct',
      modelProperties: {
        id: {
          required: false,
          readOnly: true,
          serializedName: 'id',
          type: {
            name: 'String'
          }
        },
        type: {
          required: false,
          readOnly: true,
          serializedName: 'type',
          type: {
            name: 'String'
          }
        },
        tags: {
          required: false,
          serializedName: 'tags',
          type: {
            name: 'Dictionary',
            value: {
                required: false,
                serializedName: 'StringElementType',
                type: {
                  name: 'String'
                }
            }
          }
        },
        location: {
          required: false,
          serializedName: 'location',
          type: {
            name: 'String'
          }
        },
        name: {
          required: false,
          readOnly: true,
          serializedName: 'name',
          type: {
            name: 'String'
          }
        },
        pname: {
          required: false,
          serializedName: 'properties.pname',
          type: {
            name: 'String'
          }
        },
        lsize: {
          required: false,
          serializedName: 'properties.lsize',
          type: {
            name: 'Number'
          }
        },
        provisioningState: {
          required: false,
          serializedName: 'properties.provisioningState',
          type: {
            name: 'String'
          }
        }
      }
    }
  };
};

module.exports = FlattenedProduct;