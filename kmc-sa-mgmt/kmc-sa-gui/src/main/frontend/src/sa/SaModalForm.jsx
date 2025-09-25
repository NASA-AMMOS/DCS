/*
 * Copyright 2022, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology
 * Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. By accepting
 * this software, the user agrees to comply with all applicable U.S.
 * export laws and regulations. User has the responsibility to obtain
 * export licenses, or other export authority as may be required before
 * exporting such information to foreign countries or providing access to
 * foreign persons.
 */
/**
 * @author panjames
 */
import {useFormik} from "formik";
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    InputAdornment,
    MenuItem,
    TextField,
    Toolbar,
    Typography
} from "@mui/material";
import * as yup from 'yup';
import {
    AUTH_CIPHERS,
    AUTHENTICATED_ENCRYPTION,
    AUTHENTICATION,
    ENC_CYPHERS,
    ENCRYPTION,
    intAcsLookup,
    intEcsLookup,
    KEYED,
    OPERATIONAL,
    PLAINTEXT,
    serviceTypeLookupInt,
    serviceTypeLookupStr,
    stateLookupStr,
    UNKEYED_EXPIRED
} from "./utilities";
import {useMemo} from "react";
import {useSnackbar} from "notistack";
import {createErrorCallback, createSa, deleteSa, expireSa, startSa, stopSa, updateSa} from "./api";
import EditControlStack from "./EditControlStack.jsx";
import ErrorBoundary from "./ErrorBoundary.jsx";

export const hexString = yup.string().matches(/^([\da-fA-F]{2})*$/, 'Must be a hex string that contains an even number of hex digits')

const validationSchema = yup.object({
    spi: yup.number().integer('must be an integer').min(0).required('SPI is required'),
    scid: yup.number().integer('must be an integer').min(0).required('SCID is required'),
    vcid: yup.number().integer('must be an integer').min(0).required('VCID is required'),
    tfvn: yup.number().integer('must be an integer').min(0).required('TFVN is required'),
    mapid: yup.number().integer('must be an integer').min(0).required('MAPID is required'),
    saState: yup.number().integer('must be an integer').min(0).max(3).required('state is required'),
    ekid: yup.string()
        .when(['serviceType', 'saState'], {
            is: (serviceType, saState) => saState === stateLookupStr(UNKEYED_EXPIRED) || serviceType === PLAINTEXT || serviceType === AUTHENTICATION,
            then: schema => schema.nullable(),
            otherwise: schema => schema.required("EKID is required when Service Type is ENCRYPTION or AUTHENTICATED_ENCRYPTION")
        }),
    akid: yup.string()
        .when(['serviceType', 'saState'], {
            is: (serviceType, saState) => saState === stateLookupStr(UNKEYED_EXPIRED) || serviceType === PLAINTEXT || serviceType === ENCRYPTION || serviceType === AUTHENTICATED_ENCRYPTION,
            then: schema => schema.nullable(),
            otherwise: schema => schema.required("AKID is required when Service Type is AUTHENTICATION")
        }),
    serviceType: yup.string().oneOf([PLAINTEXT, ENCRYPTION, AUTHENTICATION, AUTHENTICATED_ENCRYPTION]),
    shivfLen: yup.number().min(0),
    shsnfLen: yup.number().min(0),
    shplfLen: yup.number().min(0),
    stmacfLen: yup.number().min(0),
    ecsLen: yup.number().min(0),
    ecs: hexString.when('serviceType', {
            is: val => val === ENCRYPTION || val === AUTHENTICATED_ENCRYPTION,
            then: schema => schema.notOneOf([0, '00', '0x00'], 'When Service Type is ENCRYPTION or AUTHENTICATED_ENCRYPTION, ECS cannot be None')
        }
    ),
    ivLen: yup.number().min(0),
    iv: hexString.nullable(),
    acsLen: yup.number().min(0),
    acs: hexString.when('serviceType', {
        is: val => val === AUTHENTICATION,
        then: schema => schema.notOneOf([0, '00', '0x00'], 'When Service Type is AUTHENTICATION, ACS cannot be None')
    }),
    abmLen: yup.number().min(0),
    abm: hexString,
    arsnLen: yup.number().min(0),
    arsn: hexString,
    arsnw: yup.number().min(0),
})

export default function SaModalForm({
                                        type,
                                        data,
                                        refresh,
                                        isUpdate,
                                        rowOpen,
                                        onClose
                                    }) {

    const {enqueueSnackbar} = useSnackbar()

    const createResponseCallback = (msg) => {
        return (resp) => {
            if (resp.status === 200) {
                refresh()
                enqueueSnackbar(msg, {variant: "success"})
            }
        }
    }

    const errorCallback = createErrorCallback(enqueueSnackbar)

    const createAction = (values) => {
        createSa(type, values, createResponseCallback(`SADB ${values.spi}/${values.scid} has been created`), errorCallback)
    }

    const startAction = () => {
        startSa(type, data.id, createResponseCallback(`Started SA ${data.id.spi}/${data.id.scid}`), errorCallback)
    }

    const stopAction = (id) => {
        stopSa(type, id, createResponseCallback(`Stopped SA ${id.spi}/${id.scid}`), errorCallback)
    }

    const updateAction = (values) => {
        updateSa(type, values, createResponseCallback(`Updated SA ${values.spi}/${values.scid}`), errorCallback)
    }

    function deleteAction(id) {
        deleteSa(type, id, createResponseCallback(`SADB ${id.spi}/${id.scid} has been deleted`), errorCallback)
    }

    function expireAction(id) {
        expireSa(type, id, createResponseCallback(`Expired SA ${id.spi}/${id.scid}`), errorCallback)
    }

    const defaultValues = useMemo(() => {
        return {
            id: {
                spi: 0,
                scid: 0
            },
            spi: 0,
            scid: 0,
            vcid: 0,
            tfvn: 0,
            mapid: 0,
            saState: 1,
            ekid: null,
            akid: null,
            est: 0,
            ast: 0,
            serviceType: PLAINTEXT,
            shivfLen: 0,
            shsnfLen: 0,
            shplfLen: 0,
            stmacfLen: 0,
            ecsLen: 1,
            ecs: "01",
            ivLen: 12,
            iv: null,
            acsLen: 1,
            acs: "00",
            abmLen: 19,
            abm: "0000FC0000FFFF000000000000000000000000",
            arsnLen: 20,
            arsn: "0000000000000000000000000000000000000000",
            arsnw: 0
        }
    }, [])

    const formik = useFormik({
        initialValues: data === undefined ? defaultValues : data,
        validationSchema: validationSchema,
        onSubmit: (values) => {
            console.log("submitting")
            if (isUpdate && !formik.dirty) {
                enqueueSnackbar("Submission is unchanged, skipping update", {variant: "warning"})
                onClose()
                return
            }
            let clone = structuredClone(values)
            if (isUpdate) {
                updateAction(clone)
            } else {
                createAction(clone)
            }
            onClose()
        },
        enableReinitialize: true
    })

    function handleEcsChange(e) {
        const ecs = intEcsLookup(e.target.value)
        formik.setFieldValue('ecs', ecs.hex)
    }

    function handleAcsChange(e) {
        const acs = intAcsLookup(e.target.value)
        formik.setFieldValue('acs', acs.hex)
    }

    function handleServiceTypeChange(e) {
        const st = serviceTypeLookupInt(e.target.value)
        formik.setFieldValue("serviceType", st)
        switch (st) {
            case PLAINTEXT:
                formik.setFieldValue('est', 0)
                formik.setFieldValue('ast', 0)
                break
            case ENCRYPTION:
                formik.setFieldValue('est', 1)
                formik.setFieldValue('ast', 0)
                break
            case AUTHENTICATION:
                formik.setFieldValue('est', 0)
                formik.setFieldValue('ast', 1)
                break
            case AUTHENTICATED_ENCRYPTION:
                formik.setFieldValue('est', 1)
                formik.setFieldValue('ast', 1)
                break
            default:
        }
    }

    function handleSpiChange(e) {
        const spi = e.target.value
        formik.setFieldValue("spi", spi)
        formik.setFieldValue("id.spi", spi)
    }

    function handleScidChange(e) {
        const scid = e.target.value
        formik.setFieldValue("scid", scid)
        formik.setFieldValue("id.scid", scid)
    }

    return (
        <Dialog
            open={rowOpen}
            onClose={onClose}
            maxWidth={"md"}
            fullWidth={true}
        >
            <ErrorBoundary>
                <DialogTitle sx={{padding: 0}}>{
                    isUpdate ? (
                        <Toolbar>
                            <Typography variant={"h6"} sx={{flexGrow: 1}} noWrap>Edit SA</Typography>
                            <Box sx={{flexGrow: 0}}>
                                <EditControlStack
                                    setModalOpen={onClose}
                                    refresh={refresh}
                                    validationSchema={validationSchema}
                                    defaultValues={defaultValues}
                                    data={formik.initialValues}
                                    id={formik.initialValues.id}
                                    saState={formik.initialValues.saState}
                                    formik={formik}
                                    deleteAction={deleteAction}
                                    stopAction={stopAction}
                                    startAction={startAction}
                                    expireAction={expireAction}
                                />
                            </Box>
                        </Toolbar>
                    ) : (
                        <Toolbar><Typography variant={"h6"} sx={{flexGrow: 1}} noWrap>Create SA</Typography></Toolbar>
                    )}</DialogTitle>
                <Divider/>
                <DialogContent>
                    <div>
                        <form onSubmit={formik.handleSubmit}>
                            <SaTextField
                                formik={formik}
                                id={"spi"}
                                name={"spi"}
                                label={"SPI"}
                                disabled={isUpdate}
                                onChange={handleSpiChange}
                                helperText={"Security Parameter Index"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"scid"}
                                name={"scid"}
                                label={"SCID"}
                                disabled={isUpdate}
                                onChange={handleScidChange}
                                helperText={"Spacecraft ID"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"vcid"}
                                name={"vcid"}
                                label={"VCID"}
                                helperText={"Virtual Channel ID"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"tfvn"}
                                name={"tfvn"}
                                label={"TFVN"}
                                helperText={"Transfer Frame Version Number"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"mapid"}
                                name={"mapid"}
                                label={"MAPID"}
                                helperText={"Multiplexer Access Point ID"}
                            />
                            <SaSelectField
                                id={"saState"}
                                name={"saState"}
                                label={"State"}
                                formik={formik}
                                onChange={formik.handleChange}
                                helperText={"SA State: Unkeyed or Expired = 1, Keyed = 2, Operational = 3"}
                            >
                                <MenuItem value={1}>{UNKEYED_EXPIRED}</MenuItem>
                                <MenuItem value={2}>{KEYED}</MenuItem>
                                <MenuItem value={3}>{OPERATIONAL}</MenuItem>
                            </SaSelectField>
                            <SaSelectField
                                id={"serviceType"}
                                name={"serviceType"}
                                label={"Service Type"}
                                formik={formik}
                                onChange={handleServiceTypeChange}
                                lookup={serviceTypeLookupStr}
                                helperText={"Service Type: 0 = PLAINTEXT, 1 = AUTHENTICATION, 2 = ENCRYPTION, 3 = AUTHENTICATED_ENCRYPTION"}
                            >
                                <MenuItem value={0}>{PLAINTEXT}</MenuItem>
                                <MenuItem value={1}>{AUTHENTICATION}</MenuItem>
                                <MenuItem value={2}>{ENCRYPTION}</MenuItem>
                                <MenuItem value={3}>{AUTHENTICATED_ENCRYPTION}</MenuItem>
                            </SaSelectField>
                            <SaTextField
                                formik={formik}
                                id={"ekid"}
                                name={"ekid"}
                                label={"EKID"}
                                helperText={"Encryption Key ID"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"akid"}
                                name={"akid"}
                                label={"AKID"}
                                helperText={"Authentication Key ID"}
                            />
                            <SaSelectField
                                id={"ecs"}
                                name={"ecs"}
                                label={"ECS"}
                                formik={formik}
                                lookup={parseInt}
                                onChange={handleEcsChange}
                                helperText={`Encryption Cipher Suite: ${ENC_CYPHERS.NONE.name} = ${ENC_CYPHERS.NONE.value}, 
                            ${ENC_CYPHERS.AES256_GCM.name} = ${ENC_CYPHERS.AES256_GCM.value},
                            ${ENC_CYPHERS.AES256_CBC} = ${ENC_CYPHERS.AES256_CBC.value}`}

                            >
                                <MenuItem value={0}>{ENC_CYPHERS.NONE.name}</MenuItem>
                                <MenuItem value={1}>{ENC_CYPHERS.AES256_GCM.name}</MenuItem>
                            </SaSelectField>
                            <SaSelectField
                                id={"acs"}
                                name={"acs"}
                                label={"ACS"}
                                formik={formik}
                                lookup={parseInt}
                                onChange={handleAcsChange}
                                helperText={`Authentication Cipher Suite: ${AUTH_CIPHERS.NONE.name} = ${AUTH_CIPHERS.NONE.value}, 
                            ${AUTH_CIPHERS.AES256_CMAC.name} = ${AUTH_CIPHERS.AES256_CMAC.value}, 
                            ${AUTH_CIPHERS.HMAC_SHA256.name} = ${AUTH_CIPHERS.HMAC_SHA256.value}, 
                            ${AUTH_CIPHERS.HMAC_SHA512.name} = ${AUTH_CIPHERS.HMAC_SHA512.value}`}
                            >
                                <MenuItem value={0}>{AUTH_CIPHERS.NONE.name}</MenuItem>
                                <MenuItem value={1}>{AUTH_CIPHERS.AES256_CMAC.name}</MenuItem>
                                <MenuItem value={2}>{AUTH_CIPHERS.HMAC_SHA256.name}</MenuItem>
                                <MenuItem value={3}>{AUTH_CIPHERS.HMAC_SHA512.name}</MenuItem>
                            </SaSelectField>
                            <SaTextField
                                formik={formik}
                                id={"shivfLen"}
                                name={"shivfLen"}
                                label={"SHIVF Length"}
                                helperText={"Security Header Initialization Vector Field Length"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"shsnfLen"}
                                name={"shsnfLen"}
                                label={"SHSNF Length"}
                                helperText={"Security Header SN Field Length"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"shplfLen"}
                                name={"shplfLen"}
                                label={"SHPLF Length"}
                                helperText={"Security Header PL Field Length"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"stmacfLen"}
                                name={"stmacfLen"}
                                label={"STMACF Length"}
                                helperText={"Security Trailer MAC Field Length"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"ivLen"}
                                name={"ivLen"}
                                label={"IV Length"}
                                helperText={"Initialization Vector Length"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"iv"}
                                name={"iv"}
                                label={"IV"}
                                helperText={"Initialization Vector"}
                                InputProps={{
                                    startAdornment: <InputAdornment position="start"
                                                                    sx={{style: {fontFamily: 'Roboto Mono'}}}><Typography
                                        color={'warning.main'} fontWeight={'bold'}
                                        fontFamily={'Roboto Mono'}>0x</Typography></InputAdornment>
                                }}
                            />
                            <SaTextField
                                formik={formik}
                                id={"abmLen"}
                                name={"abmLen"}
                                label={"ABM Length"}
                                helperText={"Application Binary Mask Length"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"abm"}
                                name={"abm"}
                                label={"ABM"}
                                helperText={"Application Binary Mask"}
                                InputProps={{
                                    startAdornment: <InputAdornment position="start"
                                                                    sx={{style: {fontFamily: 'Roboto Mono'}}}><Typography
                                        color={'warning.main'} fontWeight={'bold'}
                                        fontFamily={'Roboto Mono'}>0x</Typography></InputAdornment>
                                }}
                                multiline
                            />
                            <SaTextField
                                formik={formik}
                                id={"arsnLen"}
                                name={"arsnLen"}
                                label={"ARSN Length"}
                                helperText={"Anti-Replay Sequence Number Length"}
                            />
                            <SaTextField
                                formik={formik}
                                id={"arsn"}
                                name={"arsn"}
                                label={"ARSN"}
                                helperText={"Anti-Replay Sequence Number"}
                                InputProps={{
                                    startAdornment: <InputAdornment position="start"
                                                                    sx={{style: {fontFamily: 'Roboto Mono'}}}><Typography
                                        color={'warning.main'} fontWeight={'bold'}
                                        fontFamily={'Roboto Mono'}>0x</Typography></InputAdornment>
                                }}
                            />
                            <SaTextField
                                formik={formik}
                                id={"arsnw"}
                                name={"arsnw"}
                                label={"ARSNW"}
                                helperText={"Anti-Replay Sequence Number Window"}
                            />
                        </form>
                    </div>
                </DialogContent>
                <Divider/>
                <DialogActions sx={{paddingTop: '10px'}}>
                    <Button color={"primary"} variant={"contained"} type={"submit"}
                            onClick={formik.handleSubmit}
                    >{isUpdate ? 'Update' : 'Create'}</Button>
                    <Button variant={"contained"} onClick={onClose}>Cancel</Button>
                </DialogActions>
            </ErrorBoundary>
        </Dialog>
    )
}

export function SaTextField({id, name, label, formik, onBlur, helperText, onChange, disabled, InputProps, multiline}) {
    return (
        <ErrorBoundary>
            <TextField
                margin={"dense"}
                fullWidth
                id={id}
                name={name}
                label={label}
                value={id in formik.values ? formik.values[id] == null ? '' : formik.values[id] : ''}
                onChange={onChange ? (e) => onChange(e) : (e) => formik.handleChange(e)}
                onBlur={onBlur || formik.handleBlur}
                error={formik.touched[id] && Boolean(formik.errors[id])}
                helperText={(formik.touched[id] && formik.errors[id]) || helperText}
                disabled={disabled || false}
                inputProps={{style: {fontFamily: 'Roboto Mono'}}}
                InputProps={InputProps}
                multiline={multiline || false}
                rows={multiline ? 15 : ''}
            />
        </ErrorBoundary>
    )
}

export function SaSelectField({children, id, name, label, formik, onChange, helperText, lookup}) {

    return (
        <ErrorBoundary>
            <TextField
                sx={{marginY: 1}}
                fullWidth
                id={id}
                name={name}
                label={label}
                value={formik.values[id] == null ? '' : lookup !== undefined ? lookup(formik.values[id]) : formik.values[id]}
                onChange={onChange}
                onBlur={formik.handleBlur}
                error={formik.touched[id] && Boolean(formik.errors[id])}
                helperText={(formik.touched[id] && formik.errors[id]) ||
                    helperText}
                select
                SelectProps={{style: {fontFamily: 'Roboto Mono'}}}
            >
                {children}
            </TextField>
        </ErrorBoundary>
    )
}